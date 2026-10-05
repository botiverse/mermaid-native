#!/usr/bin/env python3
"""Complete scoreLayout original file, including four segment-crossing tests."""
import argparse,base64,hashlib,json,os,shutil,subprocess,math
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-layout-quality-audit';w.mkdir(exist_ok=True)
manifest=json.loads((h/'sources.json').read_text())
for f,sha in manifest['files'].items():
 for path in [u/f,r/'compatibility/upstream-batch/originals'/f]:assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';
import * as scorer from '../packages/mermaid/src/rendering-util/layout-algorithms/layout-utils/scoreLayout.js';
import * as geometry from '../packages/mermaid/src/rendering-util/layout-algorithms/layout-utils/geometry.js';
const encode=(_,v)=>typeof v==='number'&&!Number.isFinite(v)?{$number:String(v)}:v;
const methods=[[scorer,'scoreLayout','score'],[geometry,'segmentsCross','cross']];
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
let depth=0;for(const [owner,method,op]of methods){const original=owner[method];vi.spyOn(owner,method).mockImplementation((...args)=>{const outer=depth===0;depth++;let result;try{result=original(...args);}finally{depth--;}if(outer)appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,op,args,reference:result},encode)+'\\n');return result;});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-layout-quality-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(manifest['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-layout-quality-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w')as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-layout-quality-audit/{mode}.config.ts','--workspace',f'.native-layout-quality-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed; see official.log'
assert json.loads((w/'official.json').read_text())['numPassedTests']==17
jars=[]
for module in ['mermaid-core','mermaid-layout-api','mermaid-layout-simple']:
 jar=w/(module+'.jar');shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar);jars.append(jar)
cp=os.pathsep.join(map(str,[w,*jars,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'LayoutQualityBridge.java'),str(r/'compatibility/upstream-layout-validation/LayoutValidationBridge.java'),str(r/'compatibility/upstream-tree-documents/TreeDocumentsBridge.java'),str(r/'compatibility/upstream-usecase-document/UsecaseDocumentBridge.java'),str(r/'compatibility/upstream-usecase-ast/UsecaseAstNativeBridge.java')],check=True)
trace=[json.loads(x)for x in calls.read_text().splitlines()]
res=subprocess.run(['java','-cp',cp,'LayoutQualityBridge'],input='\n'.join(base64.b64encode(json.dumps({'op':x['op'],'args':x['args']}).encode()).decode()for x in trace)+'\n',text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x)for x in res.stdout.splitlines()];assert len(trace)==len(snapshots)
def equal(a,b):
 if isinstance(a,dict)and isinstance(b,dict):return a.keys()==b.keys()and all(equal(a[k],b[k])for k in a)
 if isinstance(a,list)and isinstance(b,list):return len(a)==len(b)and all(equal(x,y)for x,y in zip(a,b))
 if isinstance(a,(int,float))and not isinstance(a,bool)and isinstance(b,(int,float))and not isinstance(b,bool):return math.isclose(a,b,rel_tol=1e-12,abs_tol=1e-12)
 return a==b
for call,snap in zip(trace,snapshots):assert equal(call['reference'],snap),(call['test'],call['reference'],snap)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'result':snap}for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const revive=(_,v)=>v&&typeof v==='object'&&'$number'in v?Number(v.$number):v;
const rows=JSON.parse(readFileSync(CACHE,'utf8')),byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
for(const [owner,method,op]of methods)vi.spyOn(owner,method).mockImplementation((...args)=>{const key=expect.getState().currentTestName;const row=byTest.get(key)?.shift();if(!row||row.op!==op||JSON.stringify(row.args,encode)!==JSON.stringify(args,encode))throw new Error('Native trace mismatch '+key);return JSON.parse(JSON.stringify(row.result),revive);});
afterEach(()=>{const key=expect.getState().currentTestName;if(byTest.get(key)?.length)throw new Error('Unconsumed Native operations: '+key);});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName']for suite in json.loads((w/'native.json').read_text())['testResults']for a in suite['assertionResults']if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':manifest['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJars':{j.name:hashlib.sha256(j.read_bytes()).hexdigest()for j in jars},'nativeCalls':len(trace),'semanticDifferentials':len(trace)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k]for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations and not baseline:
 original=cache.read_text();controls={}
 mutations={'length':('returns reasonable values for all metrics','edgeLengthRatio'), 'rank':('cyclic graph (A→B→C→A) > returns NaN for rankFaithfulness','rankFaithfulness'),'threshold':('fails when value is below min threshold','threshold'),'bend':('correctly counts bends','totalBends'),'cross':('detects T-intersection where H endpoint','cross'),'shared':('excludes shared endpoint','cross')}
 try:
  for key,(needle,field)in mutations.items():
   rows=json.loads(original);targets=set()
   for row in rows:
    if needle not in row['test']:continue
    v=row['result'];targets.add(row['test'].replace(' > ',' '))
    if field=='cross':row['result']=not v
    elif field=='threshold':v['thresholdResults']['aspectRatio']['pass']=True
    else:v['scores'][field]=0
   assert len(targets)==1,(key,targets)
   cache.write_text(json.dumps(rows));test('native');assert failed()==targets,(key,failed(),targets)
   controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:
  cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
