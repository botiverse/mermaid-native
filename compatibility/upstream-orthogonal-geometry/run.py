#!/usr/bin/env python3
"""Run twelve unchanged upstream orthogonal geometry assertions against production Kotlin."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path
p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
p.add_argument('--stdlib', type=Path, required=True)
p.add_argument('--verify-result-mutations', action='store_true')
o = p.parse_args()
u, r, h = o.upstream.resolve(), o.repo.resolve(), Path(__file__).resolve().parent
w = u / '.native-orthogonal-geometry-audit'; w.mkdir(exist_ok=True)
m = json.loads((h/'sources.json').read_text())
for group in ['files', 'sourceFiles']:
    for f, sha in m[group].items():
        assert hashlib.sha256((u/f).read_bytes()).hexdigest() == sha, f
jars = []
for module in ['mermaid-core', 'mermaid-layout-api', 'mermaid-layout-simple']:
    dest = w/(module+'.jar')
    shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', dest)
    jars.append(dest)
cp = os.pathsep.join(map(str, [w, o.stdlib.resolve(), *jars]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h/'OrthogonalGeometryNativeBridge.java')], check=True)
calls = w/'official-calls.jsonl'; calls.write_text('')
shared = """import {vi,expect} from 'vitest';
import * as geometry from '../packages/mermaid/src/rendering-util/layout-algorithms/swimlanes/direction/geometry.js';
const operations=['orthogonalSegmentsCross','orthogonalSegmentsStrictlyCross','sameAxisSegmentsOverlap','segmentHitsAnyRect','segmentConflictsWithAnyEdge','collectRealNodeBounds','classifyThreeSegmentRoute'];
function input(op,args){
 if(op==='segmentConflictsWithAnyEdge')return {op,args:[args[0],args[1],args[2],args[2].indexOf(args[3]),args[4]??{}]};
 return {op,args};
}
"""
(w/'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
for(const op of operations){const original=geometry[op];vi.spyOn(geometry,op).mockImplementation((...args)=>{
 appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify(input(op,args))})+'\\n');
 return original(...args);
});}
""".replace('CALLS',json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';
import jison from '../.vite/jisonPlugin.js';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
export default defineConfig({plugins:[jison(),jsonSchemaPlugin()],define:{'injected.includeLargeFeatures':'true','injected.profiling':'false','import.meta.vitest':'undefined',packageVersion:JSON.stringify('0.0.0')},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-orthogonal-geometry-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
    (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
    (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-orthogonal-geometry-audit/"+mode+".config.ts']);")
def test(mode):
    with (w/f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-orthogonal-geometry-audit/{mode}.config.ts','--workspace',f'.native-orthogonal-geometry-audit/{mode}.workspace.ts','--testNamePattern','^swimlane direction geometry ','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode: raise SystemExit('Original orthogonal-geometry assertions failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==12
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
lines=[]
def point(p):return [p['x'],p['y']]
def encoded(s):return base64.b64encode(s.encode()).decode()
for source in sources:
    v=json.loads(source);op=v['op'];args=v['args']
    if op in ['orthogonalSegmentsCross','orthogonalSegmentsStrictlyCross','sameAxisSegmentsOverlap']:
        row=[0 if op=='orthogonalSegmentsCross' else 4 if op=='orthogonalSegmentsStrictlyCross' else 1]+[n for p in args[:4] for n in point(p)]+[args[4] if len(args)>4 else .001]
        if op=='orthogonalSegmentsCross':row += [args[5] if len(args)>5 else .000001]
    elif op=='segmentHitsAnyRect':
        row=[2]+point(args[0])+point(args[1])+[len(args[2])]
        for entry in args[2]:
            r=entry['rect'];row += [encoded(entry['id']),r['left'],r['top'],r['right']-r['left'],r['bottom']-r['top']]
        excluded=args[3] if len(args)>3 else [];row += [len(excluded)]+[encoded(x) for x in excluded]+[args[4] if len(args)>4 else 0]
    elif op=='classifyThreeSegmentRoute':
        points=args[0] or [];row=[5,len(points)]+[n for p in points for n in point(p)]+[args[1] if len(args)>1 else .001]
    elif op=='collectRealNodeBounds':
        row=[6,len(args[0])]
        for n in args[0]:row += [encoded(n.get('id','')),n.get('x',0),n.get('y',0),n.get('width',0),n.get('height',0),int(n.get('isGroup',False)),int(n.get('isEdgeLabel',False))]
    else:
        opts=args[4];row=[3]+point(args[0])+point(args[1])+[opts.get('epsilon',.001),int(opts.get('skipDegenerateOther',False)),len(args[2]),args[3]]
        for e in args[2]:row += [int(e.get('isLayoutOnly',False)),len(e.get('points',[]))]+[n for p in e.get('points',[]) for n in point(p)]
    lines.append('\t'.join(map(str,row)))
result=subprocess.run(['java','-cp',cp,'OrthogonalGeometryNativeBridge'],input='\n'.join(lines)+'\n',text=True,capture_output=True,check=True,timeout=60)
results=[json.loads(line) for line in result.stdout.splitlines()]
assert len(results)==len(sources)
cache=w/'native-results.json';cache.write_text(json.dumps(list(zip(sources,results))))
(w/'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
for(const op of operations)vi.spyOn(geometry,op).mockImplementation((...args)=>{
 const key=JSON.stringify(input(op,args));if(!results.has(key))throw new Error('Missing Native geometry result');
 const value=results.get(key).value;
 if(op==='classifyThreeSegmentRoute')return value??undefined;
 if(op==='collectRealNodeBounds')return {nodeInfoById:new Map(value.realNodeRects.map(r=>[r.id,r])),realNodeRects:value.realNodeRects};
 return value;
});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
summary={'upstreamRevision':m['revision'],'runtimeSha256':{j.name:hashlib.sha256(j.read_bytes()).hexdigest() for j in jars},'uniqueInputs':len(sources),'selection':'All twelve original geometry tests; bounds and route classification consumed by production flow routing'}
for mode in ['official','native']:
    d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if status.returncode==0 and o.verify_result_mutations:
    original=cache.read_text();mutations={}
    try:
        for name in ['routeKind','diagonalRoute','nodeIdentity','nodeBounds']:
            altered=json.loads(original);changed=0
            for source,value in altered:
                op=json.loads(source)['op'];v=value['value']
                if name=='routeKind' and op=='classifyThreeSegmentRoute' and v and v['kind']=='HVH':v['kind']='VHV';changed+=1
                elif name=='diagonalRoute' and op=='classifyThreeSegmentRoute' and v is None:value['value']={'kind':'HVH'};changed+=1
                elif name=='nodeIdentity' and op=='collectRealNodeBounds':v['realNodeRects'][0]['id']='wrong';changed+=1
                elif name=='nodeBounds' and op=='collectRealNodeBounds':v['realNodeRects'][0]['rect']['left']+=1;changed+=1
            assert changed==1
            cache.write_text(json.dumps(altered));rc=test('native');d=json.loads((w/'native.json').read_text())
            failures=[a['fullName'] for suite in d['testResults'] for a in suite['assertionResults'] if a['status']=='failed']
            expected='classifies 4-point' if name in ['routeKind','diagonalRoute'] else 'collects only real visible nodes'
            assert rc.returncode!=0 and len(failures)==1 and expected in failures[0],failures
            mutations[name]={'failedTests':failures,'passedTests':d['numPassedTests']}
            shutil.copy2(w/'native.json',w/f'mutation-{name}.json')
    finally:
        cache.write_text(original);status=test('native')
    assert status.returncode==0
    summary['resultMutations']=mutations
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
