#!/usr/bin/env python3
"""Replay unchanged Radar model and relative-radius assertions through production Kotlin."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-radar-model-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
layout=r/'mermaid-layout-simple/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar'
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve(),layout,r/'mermaid-layout-api/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar']))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'RadarModelNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
imports="""import {vi,expect} from 'vitest';
import {db} from '../packages/mermaid/src/diagrams/radar/db.js';
import {parser} from '../packages/mermaid/src/diagrams/radar/parser.js';
import * as renderer from '../packages/mermaid/src/diagrams/radar/renderer.js';
"""
(w/'capture.ts').write_text(imports+"""import {appendFileSync} from 'node:fs';
function record(source){appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\n');}
const parse=parser.parse;vi.spyOn(parser,'parse').mockImplementation(source=>{record(source);return parse(source);});
const relative=renderer.relativeRadius;vi.spyOn(renderer,'relativeRadius').mockImplementation((...args)=>{record('R|'+args.join('|'));return relative(...args);});
""".replace('CALLS',json.dumps(str(calls))).replace("+'\n'","+'\\n'"))
config="""import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-radar-model-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-radar-model-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-radar-model-audit/{mode}.config.ts','--workspace',f'.native-radar-model-audit/{mode}.workspace.ts','--testNamePattern','^(?!.*(?:closedRoundCurve|should draw a)).*$','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,env={**os.environ,'TZ':'UTC'},stdout=f,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original Radar model suite failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==15,'Expected exactly 15 selected original assertions'
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'RadarModelNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True,timeout=60)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text(imports+"""import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));let state={};
function lookup(source){if(!models.has(source))throw new Error('Missing Native result');const result=models.get(source);if(result?.error)throw new Error(result.error);return structuredClone(result);}
vi.spyOn(db,'clear').mockImplementation(()=>{state={};});
vi.spyOn(parser,'parse').mockImplementation(async source=>{state=lookup(source);});
for(const getter of ['getAxes','getCurves','getOptions','getDiagramTitle','getAccTitle','getAccDescription'])vi.spyOn(db,getter).mockImplementation(()=>state[getter]);
vi.spyOn(renderer,'relativeRadius').mockImplementation((...args)=>lookup('R|'+args.join('|')));
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'layoutJarSha256':hashlib.sha256(layout.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
