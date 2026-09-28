#!/usr/bin/env python3
"""Run original Railroad grammar and real-consumer assertions through Native."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
assert o.stdlib.is_file(), o.stdlib
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-abnf-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'RailroadNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
imports="""import {vi,afterAll,expect} from 'vitest';
import {parser} from '../packages/mermaid/src/diagrams/railroad/parser/abnfParser.js';
import {db} from '../packages/mermaid/src/diagrams/railroad/railroadDb.js';
"""
(w/'capture.ts').write_text(imports+"""import {appendFileSync} from 'node:fs';
const original=parser.parse;vi.spyOn(parser,'parse').mockImplementation(source=>{appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');return original(source);});
afterAll(()=>vi.restoreAllMocks());
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-abnf-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-abnf-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-abnf-audit/{mode}.config.ts','--workspace',f'.native-abnf-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original ABNF suite failed')
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'RailroadNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True,timeout=60)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text(imports+"""import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
const model=source=>{const d=models.get(source);if(!d)throw new Error('Missing Native result');return structuredClone(d);};
let current={rules:[],title:''};vi.spyOn(db,'clear').mockImplementation(()=>{current={rules:[],title:''};});vi.spyOn(db,'getRules').mockImplementation(()=>current.rules);vi.spyOn(db,'getTitle').mockImplementation(()=>current.title);
vi.spyOn(parser,'parse').mockImplementation(source=>{current=model(source);if(Object.hasOwn(current,'error'))throw new Error(current.error);});
afterAll(()=>vi.restoreAllMocks());
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
