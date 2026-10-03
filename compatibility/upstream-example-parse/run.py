#!/usr/bin/env python3
"""Replay unchanged original example parsing assertions against production Kotlin."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-example-parse-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'ExampleParseNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
(w/'capture.ts').write_text("""import {vi,expect} from 'vitest';import mermaid from 'mermaid';import {appendFileSync} from 'node:fs';
const original=mermaid.parse;
vi.spyOn(mermaid,'parse').mockImplementation(async(source,...args)=>{
 appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');
 return original.call(mermaid,source,...args);
});
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';import jison from '../.vite/jisonPlugin.js';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';import {resolve} from 'node:path';
export default defineConfig({plugins:[jison(),jsonSchemaPlugin()],resolve:{alias:{mermaid:resolve('packages/mermaid/src/mermaid.ts'),'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false','import.meta.vitest':'undefined',packageVersion:JSON.stringify('0.0.0')},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:['packages/examples/src/example.spec.ts'],setupFiles:['.native-example-parse-audit/SETUP.ts']}});
"""
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-example-parse-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-example-parse-audit/{mode}.config.ts','--workspace',f'.native-example-parse-audit/{mode}.workspace.ts','--testNamePattern','^examples should have valid examples ','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original example assertions failed; inspect official.log')
assert json.loads((w/'official.json').read_text())['numPassedTests']==79
captured=[json.loads(x) for x in calls.read_text().splitlines()];assert len(captured)==79
sources=[row['source'] for row in captured]
result=subprocess.run(['java','-cp',cp,'ExampleParseNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),capture_output=True,text=True,check=True,timeout=60)
values=[base64.b64decode(x).decode() for x in result.stdout.splitlines()];assert len(values)==len(sources)
cache=w/'native-results.json';cache.write_text(json.dumps(list(zip(sources,values))))
(w/'adapter.ts').write_text("""import {vi} from 'vitest';import mermaid from 'mermaid';import {readFileSync} from 'node:fs';
const results=new Map();
for(const [source,value] of JSON.parse(readFileSync(CACHE,'utf8'))){if(!results.has(source))results.set(source,[]);results.get(source).push(value);}
vi.spyOn(mermaid,'parse').mockImplementation(async source=>{
 const queue=results.get(source);if(!queue?.length)throw new Error('Missing Native parse invocation');
 const value=queue.shift();if(value!=='PASS')throw new Error(value);return true;
});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeParseCalls':len(sources),'uniqueInputs':len(set(sources))}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'failures.json').write_text(json.dumps([{'test':row['test'],'source':row['source'],'diagnostic':value} for row,value in zip(captured,values) if value!='PASS'],indent=2)+'\n')
def failed(d):return {a['fullName'] for suite in d['testResults'] for a in suite['assertionResults'] if a['status']=='failed'}
assert failed(json.loads((w/'native.json').read_text())) == {row['test'].replace(' > ',' ') for row,value in zip(captured,values) if value!='PASS'}
assert summary['native']['numPassedTests'] + summary['native']['numFailedTests'] == 79 and summary['native']['numPendingTests'] == 4
if o.verify_mutations:
 original=cache.read_text();baseline_failed=failed(json.loads((w/'native.json').read_text()));selected={}
 for i,(row,value) in enumerate(zip(captured,values)):
  family=row['test'].split('should have valid examples > ',1)[-1].split(': ',1)[0]
  if value=='PASS' and family not in selected:selected[family]=i
 mutated=json.loads(original)
 for i in selected.values():mutated[i][1]='FAIL\nInjected diagnostic'
 try:
  cache.write_text(json.dumps(mutated));mutation=test('native');d=json.loads((w/'native.json').read_text())
  expected={captured[i]['test'].replace(' > ',' ') for i in selected.values()}
  assert mutation.returncode and failed(d)==baseline_failed|expected,(failed(d),expected)
  summary['corruptionControl']={'mutatedFamilies':len(selected),'expectedNewFailures':sorted(expected),'actualNewFailures':sorted(failed(d)-baseline_failed)}
  shutil.copy2(w/'native.json',w/'mutation-families.json')
 finally:
  cache.write_text(original);restored=test('native')
 assert restored.returncode==status.returncode and failed(json.loads((w/'native.json').read_text()))==baseline_failed
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
