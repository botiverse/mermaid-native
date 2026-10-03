#!/usr/bin/env python3
"""Run all nine original Journey DB occurrences against Native document operations."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-journey-document-audit';w.mkdir(exist_ok=True)
manifest=json.loads((h/'sources.json').read_text())
for f,sha in manifest['files'].items():
 for path in [u/f,r/'compatibility/upstream-batch/originals'/f]:assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,beforeEach,afterEach} from 'vitest';
import db from '../packages/mermaid/src/diagrams/user-journey/journeyDb.js';
const counts=new Map();let key='';
beforeEach(()=>{const name=expect.getState().currentTestName??'';const occurrence=counts.get(name)??0;counts.set(name,occurrence+1);key=name+'#'+occurrence;});
const methods=['clear','addSection','addTask','getSections','getActors','getTasks','setDiagramTitle','getDiagramTitle','setAccTitle','getAccTitle','setAccDescription','getAccDescription'];
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
for(const method of methods){const original=db[method].bind(db);vi.spyOn(db,method).mockImplementation((...args)=>{appendFileSync(CALLS,JSON.stringify({test:key,op:method,args})+'\\n');return original(...args);});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';import {resolve} from 'node:path';export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-journey-document-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(manifest['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-journey-document-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-journey-document-audit/{mode}.config.ts','--workspace',f'.native-journey-document-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed; see official.log'
assert json.loads((w/'official.json').read_text())['numPassedTests']==9
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'JourneyDocumentBridge.java'),str(r/'compatibility/upstream-tree-documents/TreeDocumentsBridge.java')],check=True)
trace=[json.loads(x) for x in calls.read_text().splitlines()]
commands=['\t'.join([row['op']]+[base64.b64encode(str(x).encode()).decode() for x in row['args']]) for row in trace]
res=subprocess.run(['java','-cp',cp,'JourneyDocumentBridge'],input='\n'.join(commands)+'\n',text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x) for x in res.stdout.splitlines()];assert len(trace)==len(snapshots)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'snapshot':snap} for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8')),byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
const call=(op,args)=>{const row=byTest.get(key)?.shift();if(!row||row.op!==op||JSON.stringify(row.args)!==JSON.stringify(args))throw new Error('Native trace mismatch '+key+' '+op);if(row.snapshot.error)throw new Error(row.snapshot.error);return row.snapshot.value;};
afterEach(()=>{if(byTest.get(key)?.length)throw new Error('Unconsumed Native operations: '+key);});
for(const method of methods)vi.spyOn(db,method).mockImplementation((...args)=>call(method,args));
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():
 counts={};bad=set()
 for suite in json.loads((w/'native.json').read_text())['testResults']:
  for a in suite['assertionResults']:
   name=a['fullName'];i=counts.get(name,0);counts[name]=i+1
   if a['status']=='failed':bad.add(name+'#'+str(i))
 return bad
baseline=failed();summary={'upstreamRevision':manifest['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'nativeOperations':len(trace)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations:
 original=cache.read_text();controls={}
 try:
  for key in ['resetTasks','resetTitle','taskScore','actorOrder','sectionOrder']:
   rows=json.loads(original);targets=set()
   for row in rows:
    value=row['snapshot'].get('value');op=row['op'];name=row['test']
    if key=='resetTasks' and op=='getTasks' and 'should clear' in name:row['snapshot']['value']=[{'task':'stale'}]
    elif key=='resetTitle' and op=='getAccTitle' and 'should clear' in name:row['snapshot']['value']='stale'
    elif key=='taskScore' and op=='getTasks' and 'tasks and actors should be added' in name:value[0]['score']=0
    elif key=='actorOrder' and op=='getActors' and 'tasks and actors should be added' in name:value.reverse()
    elif key=='sectionOrder' and op=='getSections' and 'tasks and actors should be added' in name:value.reverse()
    else:continue
    targets.add(name.replace(' > ',' '))
   assert len(targets)==(2 if key in ['resetTasks','resetTitle'] else 1) and not targets&baseline,(key,targets,baseline)
   cache.write_text(json.dumps(rows));test('native');assert failed()-baseline==targets,(key,failed(),targets)
   controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:
  cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
