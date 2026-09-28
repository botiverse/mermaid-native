#!/usr/bin/env python3
"""Execute the unchanged original Architecture grammar assertions through Kotlin."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',required=True,type=Path);p.add_argument('--repo',default=Path(__file__).resolve().parents[2],type=Path);p.add_argument('--stdlib',required=True,type=Path);o=p.parse_args()
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-architecture-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,digest in m['files'].items():
 assert hashlib.sha256((u/f).read_bytes()).hexdigest()==digest,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'ArchitectureNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
imports="""import {afterAll,vi,expect} from 'vitest';
import * as module from '../packages/parser/src/language/architecture/module.js';
"""
(w/'capture.ts').write_text(imports+"""import {appendFileSync} from 'node:fs';
const create=module.createArchitectureServices;
vi.spyOn(module,'createArchitectureServices').mockImplementation((...args)=>{
 const service=create(...args);const parser=service.Architecture.parser.LangiumParser;const original=parser.parse.bind(parser);
 parser.parse=(source)=>{appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');return original(source);};return service;
});
afterAll(()=>vi.restoreAllMocks());
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,
include:['packages/parser/tests/architecture.test.ts'],setupFiles:['.native-architecture-audit/SETUP.ts']}});
"""
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
 (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-architecture-audit/"+mode+".config.ts']);\n")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-architecture-audit/{mode}.config.ts','--workspace',f'.native-architecture-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT)
if test('official').returncode:raise SystemExit('Original Architecture suite failed')
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'ArchitectureNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text(imports+"""import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
const create=module.createArchitectureServices;
vi.spyOn(module,'createArchitectureServices').mockImplementation((...args)=>{
 const service=create(...args);service.Architecture.parser.LangiumParser.parse=source=>{const model=models.get(source);if(!model)throw new Error('Missing Native result');return structuredClone(model);};return service;
});
afterAll(()=>vi.restoreAllMocks());
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
 data=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:data[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
