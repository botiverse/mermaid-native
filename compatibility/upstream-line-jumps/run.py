#!/usr/bin/env python3
"""Complete original lineJump unit/integration assertions replay Native algorithm outputs."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-line-jumps-audit';w.mkdir(exist_ok=True)
manifest=json.loads((h/'sources.json').read_text())
for f,sha in manifest['files'].items():
 for path in [u/f,r/'compatibility/upstream-batch/originals'/f]:assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';
import * as jumps from '../packages/mermaid/src/rendering-util/rendering-elements/lineJump.js';
const methods=['findEdgeIntersections','processEdgesWithJumps','isStraightPath','curveSupportsLineHops','applyLineJumpsToSvg'];
const clone=v=>JSON.parse(JSON.stringify(v,(_,v)=>v instanceof Map?[...v]:v));
function domPaths(group){return [...group.node().querySelectorAll('path')].map(p=>({id:p.getAttribute('data-id'),d:p.getAttribute('d'),dataPoints:p.getAttribute('data-points'),style:p.getAttribute('style')??'',length:typeof p.getTotalLength==='function'?p.getTotalLength():null}));}
function normalized(op,args){return op==='applyLineJumpsToSvg'?[clone(args[1]),clone(args[2]),domPaths(args[0])]:clone(args);}
function result(op,args,value){return op==='applyLineJumpsToSvg'?domPaths(args[0]).map(({id,d,style})=>({id,d,style})):clone(value);}
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
for(const op of methods){const original=jumps[op];vi.spyOn(jumps,op).mockImplementation((...args)=>{const before=normalized(op,args);const value=original(...args);appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,op,args:before,reference:result(op,args,value)})+'\\n');return value;});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';import jison from '../.vite/jisonPlugin.js';import jsonSchema from '../.vite/jsonSchemaPlugin.js';export default defineConfig({plugins:[jison(),jsonSchema()],define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:60000,include:FILES,setupFiles:['.native-line-jumps-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(manifest['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-line-jumps-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w')as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-line-jumps-audit/{mode}.config.ts','--workspace',f'.native-line-jumps-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed; see official.log'
assert json.loads((w/'official.json').read_text())['numPassedTests']==23
jars=[]
for module in ['mermaid-core','mermaid-layout-api','mermaid-layout-simple']:
 jar=w/(module+'.jar');shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar);jars.append(jar)
cp=os.pathsep.join(map(str,[w,*jars,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'LineJumpsBridge.java'),str(r/'compatibility/upstream-tree-documents/TreeDocumentsBridge.java'),str(r/'compatibility/upstream-usecase-document/UsecaseDocumentBridge.java'),str(r/'compatibility/upstream-usecase-ast/UsecaseAstNativeBridge.java')],check=True)
trace=[json.loads(x)for x in calls.read_text().splitlines()]
(w/'native-input.txt').write_text('\n'.join(base64.b64encode(json.dumps({'op':x['op'],'args':x['args']}).encode()).decode()for x in trace)+'\n')
with (w/'native-input.txt').open() as inp,(w/'native-output.jsonl').open('w') as out:subprocess.run(['java','-cp',cp,'LineJumpsBridge'],stdin=inp,stdout=out,check=True,timeout=60)
snapshots=[json.loads(x)for x in (w/'native-output.jsonl').read_text().splitlines()];assert len(trace)==len(snapshots)
def equivalent(a,b):
 if isinstance(a,(float,int)) and isinstance(b,(float,int)):return abs(a-b)<=1e-9
 if isinstance(a,list) and isinstance(b,list):return len(a)==len(b) and all(equivalent(x,y)for x,y in zip(a,b))
 if isinstance(a,dict) and isinstance(b,dict):return a.keys()==b.keys() and all(equivalent(a[k],b[k])for k in a)
 return a==b
for call,snap in zip(trace,snapshots):assert equivalent(call['reference'],snap),(call['test'],call['reference'],snap)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'result':snap}for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8')),byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
for(const op of methods)vi.spyOn(jumps,op).mockImplementation((...args)=>{const key=expect.getState().currentTestName;const row=byTest.get(key)?.shift();if(!row||row.op!==op)throw new Error('Native trace mismatch '+key);expect(normalized(op,args)).toEqual(row.args);const output=clone(row.result);if(op==='applyLineJumpsToSvg'){for(const p of args[0].node().querySelectorAll('path')){const value=output.find(x=>x.id===p.getAttribute('data-id'));if(value){p.setAttribute('d',value.d);p.setAttribute('style',value.style);}}return;}return op==='processEdgesWithJumps'?new Map(output):output;});
afterEach(()=>{const key=expect.getState().currentTestName;if(byTest.get(key)?.length)throw new Error('Unconsumed Native operations: '+key);});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName']for suite in json.loads((w/'native.json').read_text())['testResults']for a in suite['assertionResults']if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':manifest['revision'],'nativeBaseCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJars':{j.name:hashlib.sha256(j.read_bytes()).hexdigest()for j in jars},'nativeSources':{str(f.relative_to(r)):hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted((r/'mermaid-layout-simple/src/commonMain').rglob('*.kt'))},'nativeCalls':len(trace),'semanticDifferentials':len(trace),'integrationScope':'Original upstream layout produces fixture geometry; Native executes crossing detection. No full upstream-layout parity claim.'}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k]for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations and not baseline:
 original=cache.read_text();controls={}
 try:
  for key,op in [('crossing','findEdgeIntersections'),('path','processEdgesWithJumps'),('straight','isStraightPath'),('dom','applyLineJumpsToSvg')]:
   rows=json.loads(original)
   for row in rows:
    if row['op']!=op:continue
    if op=='findEdgeIntersections':row['result']=[]
    elif op=='processEdgesWithJumps':row['result']=[[id,'M0,0'] for id,d in row['result']]
    elif op=='isStraightPath':row['result']=not row['result']
    else:
     for path in row['result']:path['d']='M0,0';path['style']=''
   cache.write_text(json.dumps(rows));test('native');assert failed(),key
   controls[key]=sorted(failed());shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
