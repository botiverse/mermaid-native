#!/usr/bin/env python3
"""Run the complete unchanged validateLayout.spec.ts via Native measured geometry."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-layout-validation-audit';w.mkdir(exist_ok=True)
manifest=json.loads((h/'sources.json').read_text())
for f,sha in manifest['files'].items():
 for path in [u/f,r/'compatibility/upstream-batch/originals'/f]:assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';
import * as validator from '../packages/mermaid/src/rendering-util/layout-algorithms/layout-utils/validateLayout.js';
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
const original=validator.validateLayout;vi.spyOn(validator,'validateLayout').mockImplementation((input)=>{appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,input})+'\\n');return original(input);});
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-layout-validation-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(manifest['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-layout-validation-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w')as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-layout-validation-audit/{mode}.config.ts','--workspace',f'.native-layout-validation-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed; see official.log'
assert json.loads((w/'official.json').read_text())['numPassedTests']==39
jars=[]
for module in ['mermaid-core','mermaid-layout-api','mermaid-layout-simple']:
 jar=w/(module+'.jar');shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar);jars.append(jar)
cp=os.pathsep.join(map(str,[w,*jars,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'LayoutValidationBridge.java'),str(r/'compatibility/upstream-tree-documents/TreeDocumentsBridge.java'),str(r/'compatibility/upstream-usecase-document/UsecaseDocumentBridge.java'),str(r/'compatibility/upstream-usecase-ast/UsecaseAstNativeBridge.java')],check=True)
trace=[json.loads(x)for x in calls.read_text().splitlines()]
res=subprocess.run(['java','-cp',cp,'LayoutValidationBridge'],input='\n'.join(base64.b64encode(json.dumps(x['input']).encode()).decode()for x in trace)+'\n',text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x)for x in res.stdout.splitlines()];assert len(trace)==len(snapshots)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'result':snap}for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8')),byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
vi.spyOn(validator,'validateLayout').mockImplementation((input)=>{const key=expect.getState().currentTestName;const row=byTest.get(key)?.shift();if(!row||JSON.stringify(row.input)!==JSON.stringify(input))throw new Error('Native trace mismatch '+key);return row.result;});
afterEach(()=>{const key=expect.getState().currentTestName;if(byTest.get(key)?.length)throw new Error('Unconsumed Native operations: '+key);});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName']for suite in json.loads((w/'native.json').read_text())['testResults']for a in suite['assertionResults']if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':manifest['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJars':{j.name:hashlib.sha256(j.read_bytes()).hexdigest()for j in jars},'nativeCalls':len(trace)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k]for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations and not baseline:
 original=cache.read_text();controls={}
 mutations={
  'score':('returns 1000 for a clean tiny graph','score'),
  'crossings':('charges crossings less than','crossings'),
  'bend':('flags edge-bend-near-endpoint when the LAST','edge-bend-near-endpoint'),
  'obstacle':('flags edge-intersects-obstacle when an edge loops','edge-intersects-obstacle'),
  'port':('flags edge-shared-projected-port when a detached','edge-shared-projected-port'),
  'parallel':('flags edge-parallel-segment-too-close for long','edge-parallel-segment-too-close'),
  'label':('flags edge-label-overlaps-own-arrowhead when an overlay','edge-label-overlaps-own-arrowhead'),
  'title':('flags edge-intersects-group-title when an edge crosses','edge-intersects-group-title'),
 }
 try:
  for key,(needle,field)in mutations.items():
   rows=json.loads(original);targets=set()
   for row in rows:
    if needle not in row['test']:continue
    v=row['result'];targets.add(row['test'].replace(' > ',' '))
    if field=='score':v['score']=1
    elif field=='crossings':v['breakdown']['crossings']=0
    else:v['issues']=[i for i in v['issues']if i['type']!=field]
   assert len(targets)==1,(key,targets)
   cache.write_text(json.dumps(rows));test('native');assert failed()==targets,(key,failed(),targets)
   controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:
  cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
