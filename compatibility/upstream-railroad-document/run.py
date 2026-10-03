#!/usr/bin/env python3
"""Execute the entire pinned Railroad DB file using real Kotlin document operations."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path

p=argparse.ArgumentParser()
p.add_argument('--upstream',type=Path,required=True)
p.add_argument('--stdlib',type=Path,required=True)
p.add_argument('--verify-mutations',action='store_true')
o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve()
w=u/'.native-railroad-document-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():
    for path in (u/f,r/'compatibility/upstream-batch/originals'/f):
        assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';
import {db} from '../packages/mermaid/src/diagrams/railroad/railroadDb.js';
const name=()=>expect.getState().currentTestName??'';
const methods=['clear','addRule','getRule','getRules','setTitle','getTitle','setDiagramTitle','getDiagramTitle','setAccTitle','getAccTitle','setAccDescription','getAccDescription'];
const argsFor=(method,args)=>{if(method==='addRule'){
 const rule=args[0];if(rule.definition.type!=='terminal'||Object.keys(rule).some(k=>!['name','definition'].includes(k))||Object.keys(rule.definition).some(k=>!['type','value'].includes(k)))throw new Error('Unsupported original rule transport');
 return [rule.name,rule.definition.type,rule.definition.value];
}return args.map(x=>x==null?null:String(x));};
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
for(const method of methods){const original=db[method];vi.spyOn(db,method).mockImplementation((...args)=>{appendFileSync(CALLS,JSON.stringify({test:name(),op:method,args:argsFor(method,args)})+'\\n');return original(...args);});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-railroad-document-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
    (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
    (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-railroad-document-audit/"+mode+".config.ts']);")
def test(mode):
    with (w/f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-railroad-document-audit/{mode}.config.ts','--workspace',f'.native-railroad-document-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed'
assert json.loads((w/'official.json').read_text())['numPassedTests']==12
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'RailroadDocumentBridge.java')],check=True)
trace=[json.loads(x) for x in calls.read_text().splitlines()]
commands=['\t'.join([row['op']]+['-' if x is None else base64.b64encode(x.encode()).decode() for x in row['args']]) for row in trace]
result=subprocess.run(['java','-cp',cp,'RailroadDocumentBridge'],input='\n'.join(commands)+'\n',text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x) for x in result.stdout.splitlines()];assert len(snapshots)==len(trace)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'snapshot':snap} for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8'));const byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
for(const method of methods)vi.spyOn(db,method).mockImplementation((...args)=>{
 const row=byTest.get(name())?.shift();if(!row||row.op!==method||JSON.stringify(row.args)!==JSON.stringify(argsFor(method,args)))throw new Error('Native trace mismatch: '+name()+' '+method);
 if(row.snapshot.error)throw new Error(row.snapshot.error);const value=row.snapshot.value;return method==='getRule'&&value===null?undefined:value;
});
afterEach(()=>{if(byTest.get(name())?.length)throw new Error('Unconsumed Native operations: '+name());});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName'] for s in json.loads((w/'native.json').read_text())['testResults'] for a in s['assertionResults'] if a['status']=='failed'}
baseline=failed()
summary={'upstreamRevision':m['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'nativeOperations':len(trace),'operations':{op:sum(x['op']==op for x in trace) for op in sorted({x['op'] for x in trace})}}
for mode in ['official','native']:
    d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations:
    original=cache.read_text();controls={}
    try:
        for key in ['appendDuplicates','lookupLast','clearMetadata','titleCleanup','ruleCleanup','missingRule']:
            mutated=json.loads(original);targets=set();changed=0
            for row in mutated:
                op=row['op'];name=row['test'];snap=row['snapshot']
                if key=='appendDuplicates' and op=='getRules' and name.endswith('should handle duplicate rules'):snap['value']=snap['value'][:1]
                elif key=='lookupLast' and op=='getRule' and name.endswith('should handle duplicate rules'):snap['value']['definition']['value']='first'
                elif key=='clearMetadata' and op=='getAccTitle' and name.endswith('should clear all state including accessibility fields'):snap['value']='stale'
                elif key=='titleCleanup' and changed==0 and op=='getTitle' and name.endswith('should sanitize titles before storing them'):snap['value']='<script>bad</script>'+snap['value']
                elif key=='ruleCleanup' and op=='getRules' and name.endswith('should sanitize rule names and terminal values at the db boundary'):snap['value'][0]['name']+='<script>bad</script>'
                elif key=='missingRule' and op=='getRule' and name.endswith('should return undefined for non-existent rule'):snap['value']={'name':'wrong','definition':{'type':'terminal','value':'wrong'}}
                else:continue
                changed+=1;targets.add(name.replace(' > ',' '))
            assert changed==1 and len(targets)==1 and not targets&baseline,(key,targets,baseline)
            cache.write_text(json.dumps(mutated));test('native');assert failed()-baseline==targets,(key,failed(),targets)
            controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
    finally:
        cache.write_text(original);status=test('native');assert failed()==baseline
    summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
