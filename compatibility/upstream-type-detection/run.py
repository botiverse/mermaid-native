#!/usr/bin/env python3
"""Replay unchanged upstream type detection assertions using production Kotlin."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-type-detection-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for group in ['files','sourceFiles']:
 for f,sha in m[group].items(): assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'mermaid-core.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,o.stdlib.resolve(),jar]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'TypeDetectionNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
shared="import {vi,expect} from 'vitest';\nimport * as detection from '../packages/mermaid/src/diagram-api/detectType.js';\n"
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
const original=detection.detectType;
vi.spyOn(detection,'detectType').mockImplementation(source=>{
 appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');
 return original(source);
});
""".replace('CALLS',json.dumps(str(calls))))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text("import {defineConfig} from 'vitest/config';import jison from '../.vite/jisonPlugin.js';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';export default defineConfig({plugins:[jison(),jsonSchemaPlugin()],define:{'injected.includeLargeFeatures':'true','injected.profiling':'false','import.meta.vitest':'undefined',packageVersion:JSON.stringify('0.0.0')},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:"+json.dumps(list(m['files']))+",setupFiles:['.native-type-detection-audit/"+setup+".ts']}});")
 (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-type-detection-audit/"+mode+".config.ts']);")
def run(mode):
 with (w/f'{mode}.log').open('w') as log:
  return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-type-detection-audit/{mode}.config.ts','--workspace',f'.native-type-detection-audit/{mode}.workspace.ts','--testNamePattern','when detecting chart type.*should handle a graph definition(?: with leading spaces(?: and newline)?| for gitGraph)?$','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=180)
if run('official').returncode: raise SystemExit('Official type detection assertions failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==4
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
encoded='\n'.join(base64.b64encode(x.encode()).decode() for x in sources)+'\n'
result=subprocess.run(['java','-cp',cp,'TypeDetectionNativeBridge'],input=encoded,capture_output=True,text=True,check=True,timeout=60)
values=[base64.b64decode(line).decode() for line in result.stdout.splitlines()];assert len(values)==len(sources)
cache=w/'native-results.json';cache.write_text(json.dumps(list(zip(sources,values))))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
vi.spyOn(detection,'detectType').mockImplementation(source=>{
 if(!results.has(source))throw new Error('Missing Native detection result');
 return results.get(source);
});
""".replace('CACHE',json.dumps(str(cache))))
status=run('native');summary={'upstreamRevision':m['revision'],'runtimeSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources),'selection':'Four unchanged utils.spec.ts basic graph/gitGraph detection tests; other52 unselected'}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
