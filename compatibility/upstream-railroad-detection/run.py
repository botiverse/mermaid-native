#!/usr/bin/env python3
"""Replay unchanged Railroad detector assertions against production Kotlin."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-railroad-detection-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for group in ['files','sourceFiles']:
 for f,sha in m[group].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'mermaid-core.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,o.stdlib.resolve(),jar]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'RailroadDetectionNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect} from 'vitest';
import {railroad} from '../packages/mermaid/src/diagrams/railroad/railroadDetector.js';
import {railroadAbnf} from '../packages/mermaid/src/diagrams/railroad/abnfDetector.js';
import {railroadEbnf} from '../packages/mermaid/src/diagrams/railroad/ebnfDetector.js';
import {railroadPeg} from '../packages/mermaid/src/diagrams/railroad/pegDetector.js';
const detectors={NATIVE:railroad,ABNF:railroadAbnf,EBNF:railroadEbnf,PEG:railroadPeg};
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
for(const [kind,obj] of Object.entries(detectors)){
 const original=obj.detector;
 vi.spyOn(obj,'detector').mockImplementation((text)=>{
  appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify([kind,text])})+'\\n');
  return original(text);
 });
}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-railroad-detection-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
 (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-railroad-detection-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as log:
  return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-railroad-detection-audit/{mode}.config.ts','--workspace',f'.native-railroad-detection-audit/{mode}.workspace.ts','--testNamePattern','^(?!.*(?:loader|should load the diagram|should have correct id)).*','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=180)
if test('official').returncode:raise SystemExit('Official detector assertions failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==21
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
lines=[]
for source in sources:
 kind,text=json.loads(source);lines.append(kind+'\t'+base64.b64encode(text.encode()).decode())
result=subprocess.run(['java','-cp',cp,'RailroadDetectionNativeBridge'],input='\n'.join(lines)+'\n',capture_output=True,text=True,check=True,timeout=60)
values=result.stdout.splitlines();assert len(values)==len(sources) and all(v in ['true','false'] for v in values)
cache=w/'native-results.json';cache.write_text(json.dumps(list(zip(sources,[v=='true' for v in values]))))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
for(const [kind,obj] of Object.entries(detectors)){
 vi.spyOn(obj,'detector').mockImplementation((text)=>{
  const key=JSON.stringify([kind,text]);if(!results.has(key))throw new Error('Missing Native detector result');
  return results.get(key);
 });
}
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'runtimeSha256':{'mermaid-core.jar':hashlib.sha256(jar.read_bytes()).hexdigest()},'uniqueInputs':len(sources),'selection':'21 original detection assertions; four JS export id and seven loader assertions unselected'}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
