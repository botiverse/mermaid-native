#!/usr/bin/env python3
"""Run original Event Modeling grammar and source-type validation assertions through Native."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
assert o.stdlib.is_file(), o.stdlib
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-eventmodeling-grammar-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve(),r/'mermaid-layout-simple/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',r/'mermaid-layout-api/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar']));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'EventGrammarNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
imports="""import {vi,afterAll,expect} from 'vitest';
"""
(w/'capture.ts').write_text(imports+"""import {appendFileSync} from 'node:fs';
vi.mock('../packages/parser/tests/test-util.js',async original=>{const m=await original();return {...m,eventModelingParse:source=>{appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify(['parse',source])})+'\\n');return m.eventModelingParse(source);}}});
vi.mock('../packages/parser/src/language/eventmodeling/event-modeling-validator.js',async original=>{const m=await original();return {...m,EventModelingValidator:class extends m.EventModelingValidator {checkSourceFrameTypes(frame,accept){appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify(['validate',frame.modelEntityType,frame.sourceFrames.map(s=>s.ref?.modelEntityType).filter(Boolean)])})+'\\n');return super.checkSourceFrameTypes(frame,accept);}}};});
afterAll(()=>vi.restoreAllMocks());
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-eventmodeling-grammar-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-eventmodeling-grammar-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-eventmodeling-grammar-audit/{mode}.config.ts','--workspace',f'.native-eventmodeling-grammar-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original Event Modeling grammar suite failed')
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
def wire(key):
 a=json.loads(key)
 return ('parse\t'+base64.b64encode(a[1].encode()).decode()) if a[0]=='parse' else ('validate\t'+a[1]+'\t'+','.join(a[2]))
result=subprocess.run(['java','-cp',cp,'EventGrammarNativeBridge'],input=''.join(wire(s)+'\n' for s in sources),text=True,capture_output=True,check=True,timeout=60)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text(imports+"""import {readFileSync} from 'node:fs';
const models=vi.hoisted(()=>new Map());
for(const [key,value] of JSON.parse(readFileSync(CACHE,'utf8')))models.set(key,value);
vi.mock('../packages/parser/tests/test-util.js',async original=>{const m=await original();return {...m,eventModelingParse:source=>{const d=models.get(JSON.stringify(['parse',source]));if(!d)throw new Error('Missing Native parse');return {value:d,lexerErrors:d.error?[{message:d.error}]:[],parserErrors:[]};}}});
vi.mock('../packages/parser/src/language/eventmodeling/event-modeling-validator.js',async original=>{const m=await original();return {...m,EventModelingValidator:class extends m.EventModelingValidator {checkSourceFrameTypes(frame,accept){const d=models.get(JSON.stringify(['validate',frame.modelEntityType,frame.sourceFrames.map(s=>s.ref?.modelEntityType).filter(Boolean)]));if(!d)throw new Error('Missing Native validation');if(d.error)throw new Error(d.error);for(const message of d.errors)accept('error',message,{node:frame,property:'sourceFrames'});}}};});
afterAll(()=>vi.restoreAllMocks());
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
