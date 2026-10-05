#!/usr/bin/env python3
"""Replay the whole unchanged frontmatter file against Native values/errors, with corruption controls."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-frontmatter-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():
 for path in [u/f,r/'compatibility/upstream-batch/originals'/f]:assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';import * as frontmatter from '../packages/mermaid/src/diagram-api/frontmatter.js';
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
const original=frontmatter.extractFrontMatter;vi.spyOn(frontmatter,'extractFrontMatter').mockImplementation(source=>{let reference;try{reference={value:original(source)};}catch(e){reference={error:e.message};}appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source,reference})+'\\n');if('error'in reference)throw new Error(reference.error);return reference.value;});
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-frontmatter-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-frontmatter-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w')as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-frontmatter-audit/{mode}.config.ts','--workspace',f'.native-frontmatter-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed; see official.log'
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'FrontmatterBridge.java'),str(r/'compatibility/upstream-tree-documents/TreeDocumentsBridge.java'),str(r/'compatibility/upstream-usecase-document/UsecaseDocumentBridge.java'),str(r/'compatibility/upstream-usecase-ast/UsecaseAstNativeBridge.java')],check=True)
trace=[json.loads(x)for x in calls.read_text().splitlines()]
res=subprocess.run(['java','-cp',cp,'FrontmatterBridge'],input='\n'.join(base64.b64encode(x['source'].encode()).decode()for x in trace)+'\n',text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x)for x in res.stdout.splitlines()];assert len(trace)==len(snapshots)
for call,snap in zip(trace,snapshots):
 if 'error'in call['reference']:assert 'error'in snap and snap['error'] in call['reference']['error'],(call,snap)
 else:assert call['reference']==snap,(call,snap)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'result':snap}for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8')),byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
vi.spyOn(frontmatter,'extractFrontMatter').mockImplementation(source=>{const key=expect.getState().currentTestName;const row=byTest.get(key)?.shift();if(!row||row.source!==source)throw new Error('Native trace mismatch '+key);if('error'in row.result)throw new Error(row.result.error);return structuredClone(row.result.value);});
afterEach(()=>{const key=expect.getState().currentTestName;if(byTest.get(key)?.length)throw new Error('Unconsumed Native operations: '+key);});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName']for suite in json.loads((w/'native.json').read_text())['testResults']for a in suite['assertionResults']if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':m['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'nativeCalls':len(trace),'semanticDifferentials':len(trace)-1,'errorComparisons':1}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k]for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations and not baseline:
 original=cache.read_text();controls={}
 try:
  for needle in ['without mappings','multi-line string','invalid YAML','frontmatter with config','tab indentation']:
   rows=json.loads(original);targets=set()
   for row in rows:
    if needle not in row['test'] or (needle == 'frontmatter with config' and not row['test'].endswith('handles frontmatter with config')):continue
    targets.add(row['test'].replace(' > ',' '));row['result']={'value':{'text':'corrupted','metadata':{}}}
   assert len(targets)==1,(needle,targets)
   cache.write_text(json.dumps(rows));test('native');assert failed()==targets,(needle,failed(),targets);controls[needle]=sorted(targets)
 finally:cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
