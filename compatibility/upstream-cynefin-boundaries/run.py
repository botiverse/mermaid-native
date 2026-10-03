#!/usr/bin/env python3
"""Execute the entire pinned Cynefin DB and geometry file using real Kotlin document operations."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path

p=argparse.ArgumentParser()
p.add_argument('--upstream',type=Path,required=True)
p.add_argument('--stdlib',type=Path,required=True)
p.add_argument('--verify-mutations',action='store_true')
o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve()
w=u/'.native-cynefin-boundaries-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():
    for path in (u/f,r/'compatibility/upstream-batch/originals'/f):
        assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
for f,sha in m['dependencies'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';
import {db} from '../packages/mermaid/src/diagrams/cynefin/cynefinDb.js';
import * as geometry from '../packages/mermaid/src/diagrams/cynefin/cynefinBoundaries.js';
const name=()=>expect.getState().currentTestName??'';
const database=['clear','setDomains','getDomains','setTransitions','getTransitions','getConfig'];
const shapes=['seededRandom','hashString','resolveSeed','generateFoldPath','generateHorizontalBoundary','generateCliffPath','generateConfusionPath'];
const targets=[...database.map(method=>[db,method]),...shapes.map(method=>[geometry,method])];
const argsFor=(method,args)=>{
 if(method==='setDomains')return args[0]==null?[null]:[args[0].length,...args[0].flatMap(b=>[b.domain,(b.items??[]).length,...(b.items??[]).map(i=>i.label)])].map(String);
 if(method==='setTransitions')return args[0]==null?[null]:[args[0].length,...args[0].flatMap(t=>[t.from,t.to,t.label??''])].map(String);
 return args.map(x=>x==null?null:String(x));
};
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
for(const [target,method] of targets){const original=target[method];vi.spyOn(target,method).mockImplementation((...args)=>{
 const row={test:name(),op:method,args:argsFor(method,args)};const result=original(...args);if(shapes.includes(method))row.reference=result;
 appendFileSync(CALLS,JSON.stringify(row)+'\\n');return result;
});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-cynefin-boundaries-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
    (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
    (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-cynefin-boundaries-audit/"+mode+".config.ts']);")
def test(mode):
    with (w/f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-cynefin-boundaries-audit/{mode}.config.ts','--workspace',f'.native-cynefin-boundaries-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed'
assert json.loads((w/'official.json').read_text())['numPassedTests']==27
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
layout_jar=w/'layout-api.jar';shutil.copy2(r/'mermaid-layout-api/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',layout_jar)
cp=os.pathsep.join(map(str,[w,jar,layout_jar,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'CynefinNativeBridge.java')],check=True)
trace=[json.loads(x) for x in calls.read_text().splitlines()]
commands=['\t'.join([row['op']]+['-' if x is None else base64.b64encode(x.encode()).decode() for x in row['args']]) for row in trace]
result=subprocess.run(['java','-cp',cp,'CynefinNativeBridge'],input='\n'.join(commands)+'\n',text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x) for x in result.stdout.splitlines()];assert len(snapshots)==len(trace)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'snapshot':snap} for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8'));const byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
for(const [target,method] of targets)vi.spyOn(target,method).mockImplementation((...args)=>{
 const row=byTest.get(name())?.shift();if(!row||row.op!==method||JSON.stringify(row.args)!==JSON.stringify(argsFor(method,args)))throw new Error('Native trace mismatch: '+name()+' '+method);
 if(row.snapshot.error)throw new Error(row.snapshot.error);const value=row.snapshot.value;return method==='getDomains'?new Map(Object.entries(value)):value;
});
afterEach(()=>{if(byTest.get(name())?.length)throw new Error('Unconsumed Native operations: '+name());});
""".replace('CACHE',json.dumps(str(cache))))
# Original assertions mostly test shape/determinism. Also compare all executed pure
# function results to the upstream numeric geometry, tolerating only number formatting.
import math,re
numeric=r'[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?'
geometry_checks=0
for call,snapshot in zip(trace,snapshots):
    if 'reference' not in call:continue
    expected=call['reference'];actual=snapshot.get('value');assert 'error' not in snapshot,snapshot
    if isinstance(expected,(int,float)):
        assert math.isclose(expected,actual,rel_tol=0,abs_tol=1e-10),(call,actual)
    else:
        assert re.findall('[MCAZ]',expected)==re.findall('[MCAZ]',actual),(call,actual)
        x=[float(n) for n in re.findall(numeric,expected)];y=[float(n) for n in re.findall(numeric,actual)]
        assert len(x)==len(y) and all(math.isclose(a,b,rel_tol=0,abs_tol=1e-10) for a,b in zip(x,y)),(call,actual)
    geometry_checks+=1
status=test('native')
def failed():return {a['fullName'] for s in json.loads((w/'native.json').read_text())['testResults'] for a in s['assertionResults'] if a['status']=='failed'}
baseline=failed()
summary={'upstreamRevision':m['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'layoutJarSha256':hashlib.sha256(layout_jar.read_bytes()).hexdigest(),'referenceGeometryComparisons':geometry_checks,'nativeOperations':len(trace),'operations':{op:sum(x['op']==op for x in trace) for op in sorted({x['op'] for x in trace})}}
for mode in ['official','native']:
    d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations:
    original=cache.read_text();controls={}
    try:
        for key in ['emptyState','selfLoops','foldCurve','ellipseRadius','explicitSeed','randomRange']:
            mutated=json.loads(original);targets=set();changed=0
            for row in mutated:
                op=row['op'];name=row['test'];snap=row['snapshot']
                if key=='emptyState' and op=='getDomains' and name.endswith('should start empty'):snap['value']={'complex':{'name':'complex','items':[]}}
                elif key=='selfLoops' and op=='getTransitions' and name.endswith('should filter out self-loop transitions'):snap['value'].append({'from':'complex','to':'complex'})
                elif key=='foldCurve' and op=='generateFoldPath' and name.endswith('generateFoldPath should return valid SVG path'):snap['value']=snap['value'].replace('C','L')
                elif key=='ellipseRadius' and op=='generateConfusionPath' and name.endswith('generateConfusionPath should use provided center and radii'):snap['value']=snap['value'].replace('350','351')
                elif key=='explicitSeed' and changed==0 and op=='resolveSeed' and name.endswith('returns the configured seed when it is a non-zero number'):snap['value']=43
                elif key=='randomRange' and changed==0 and op=='seededRandom' and name.endswith('seededRandom should return values between 0 and 1'):snap['value']=1
                else:continue
                changed+=1;targets.add(name.replace(' > ',' '))
            assert changed==1 and len(targets)==1 and not targets&baseline,(key,targets,baseline)
            cache.write_text(json.dumps(mutated));test('native');assert failed()-baseline==targets,(key,failed(),targets)
            controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
    finally:
        cache.write_text(original);status=test('native');assert failed()==baseline
    summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
