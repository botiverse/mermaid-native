#!/usr/bin/env python3
"""Whole unchanged endpoint/polyline files; Native outputs replayed into original assertions."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-endpoint-cleanup-audit';w.mkdir(exist_ok=True)
manifest=json.loads((h/'sources.json').read_text())
for f,sha in manifest['files'].items():
 for path in [u/f,r/'compatibility/upstream-batch/originals'/f]:assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';
import * as geometry from '../packages/mermaid/src/rendering-util/layout-algorithms/swimlanes/direction/geometry.js';
import * as endpoint from '../packages/mermaid/src/rendering-util/layout-algorithms/swimlanes/direction/endpointClip.js';
const methods=[[geometry,'orthogonalizePolyline','orthogonalize'],[geometry,'simplifyPolyline','simplify'],[endpoint,'clipEdgeEndpointsToNodeBoundaries','clip'],[endpoint,'prepareEdgeEndpointsForRenderer','prepare']];
const encode=(_,v)=>v instanceof Map?[...v]:v;
const clone=v=>JSON.parse(JSON.stringify(v,encode));
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
let depth=0;for(const [owner,method,op]of methods){const original=owner[method];vi.spyOn(owner,method).mockImplementation((...args)=>{const outer=depth===0;const before=outer?clone(args):null;depth++;let result;try{result=original(...args);}finally{depth--;}if(outer)appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,op,args:before,reference:op==='clip'||op==='prepare'?args[0]:result},encode)+'\\n');return result;});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-endpoint-cleanup-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(manifest['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-endpoint-cleanup-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w')as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-endpoint-cleanup-audit/{mode}.config.ts','--workspace',f'.native-endpoint-cleanup-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed; see official.log'
assert json.loads((w/'official.json').read_text())['numPassedTests']==11
jars=[]
for module in ['mermaid-core','mermaid-layout-api','mermaid-layout-simple']:
 jar=w/(module+'.jar');shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar);jars.append(jar)
cp=os.pathsep.join(map(str,[w,*jars,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'EndpointCleanupBridge.java'),str(r/'compatibility/upstream-tree-documents/TreeDocumentsBridge.java'),str(r/'compatibility/upstream-usecase-document/UsecaseDocumentBridge.java'),str(r/'compatibility/upstream-usecase-ast/UsecaseAstNativeBridge.java')],check=True)
trace=[json.loads(x)for x in calls.read_text().splitlines()]
res=subprocess.run(['java','-cp',cp,'EndpointCleanupBridge'],input='\n'.join(base64.b64encode(json.dumps({'op':x['op'],'args':x['args']}).encode()).decode()for x in trace)+'\n',text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x)for x in res.stdout.splitlines()];assert len(trace)==len(snapshots)
for call,snap in zip(trace,snapshots):assert call['reference']==snap,(call['test'],call['reference'],snap)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'result':snap}for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8')),byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
for(const [owner,method,op]of methods)vi.spyOn(owner,method).mockImplementation((...args)=>{const key=expect.getState().currentTestName;const row=byTest.get(key)?.shift();if(!row||row.op!==op||JSON.stringify(row.args)!==JSON.stringify(args,encode))throw new Error('Native trace mismatch '+key);const result=clone(row.result);if(op==='clip'||op==='prepare'){for(let i=0;i<args[0].length;i++){if('points'in result[i])args[0][i].points=result[i].points;}return;}return result;});
afterEach(()=>{const key=expect.getState().currentTestName;if(byTest.get(key)?.length)throw new Error('Unconsumed Native operations: '+key);});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName']for suite in json.loads((w/'native.json').read_text())['testResults']for a in suite['assertionResults']if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':manifest['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJars':{j.name:hashlib.sha256(j.read_bytes()).hexdigest()for j in jars},'nativeCalls':len(trace),'semanticDifferentials':len(trace)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k]for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations and not baseline:
 original=cache.read_text();controls={}
 needles={'orientation':'preserves incoming vertical','spike':'removes out-and-back','buried':'clips buried endpoints','corner':'moves straight side-to-side','duplicates':'duplicates snapped endpoints','approach':'snaps renderer endpoints to the boundary entered'}
 try:
  for key,needle in needles.items():
   rows=json.loads(original);targets=set()
   for row in rows:
    if needle not in row['test']:continue
    targets.add(row['test'].replace(' > ',' '));points=row['result'] if row['op']in ['orthogonalize','simplify']else row['result'][0]['points'];points[0]['x']+=3
   assert len(targets)==1,(key,targets)
   cache.write_text(json.dumps(rows));test('native');assert failed()==targets,(key,failed(),targets)
   controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:
  cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
