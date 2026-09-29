#!/usr/bin/env python3
"""Replay unchanged Gantt calendar option assertions against the production Native parser."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-gantt-calendar-options-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve(),r/'mermaid-layout-simple/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',r/'mermaid-layout-api/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar']));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'GanttCalendarOptionsNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
(w/'capture.ts').write_text("""import {vi,expect} from 'vitest';import {appendFileSync} from 'node:fs';
import db from '../packages/mermaid/src/diagrams/gantt/ganttDb.js';
let diagram=['gantt'];
for(const method of ['clear','setExcludes','setIncludes']){
 const originalMethod=db[method];
 vi.spyOn(db,method).mockImplementation((...args)=>{
  if(method==='clear')diagram=['gantt'];
  else diagram.push((method==='setExcludes'?'excludes ':'includes ')+args[0]);
  return originalMethod(...args);
 });
}
for(const getter of ['getExcludes','getIncludes']){const original=db[getter];vi.spyOn(db,getter).mockImplementation(()=>{appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:diagram.join('\\n')})+'\\n');return original();});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-gantt-calendar-options-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-gantt-calendar-options-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-gantt-calendar-options-audit/{mode}.config.ts','--workspace',f'.native-gantt-calendar-options-audit/{mode}.workspace.ts', '--testNamePattern',"should (?:merge|dedupe) tokens across multiple set(?:Excludes|Includes) calls",'--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,env={**os.environ,'TZ':'UTC'},stdout=f,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original Gantt model suite failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==3, 'Expected exactly 3 selected original assertions'
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'GanttCalendarOptionsNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True,timeout=60)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text("""import {vi} from 'vitest';import {readFileSync} from 'node:fs';
import db from '../packages/mermaid/src/diagrams/gantt/ganttDb.js';
let diagram=['gantt'];
for(const method of ['clear','setExcludes','setIncludes']){
 const originalMethod=db[method];
 vi.spyOn(db,method).mockImplementation((...args)=>{
  if(method==='clear')diagram=['gantt'];
  else diagram.push((method==='setExcludes'?'excludes ':'includes ')+args[0]);
  return originalMethod(...args);
 });
}
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
for(const getter of ['getExcludes','getIncludes'])vi.spyOn(db,getter).mockImplementation(()=>{const result=models.get(diagram.join('\\n'));if(!result||result.error)throw new Error(result?.error??'Missing Native model');return result[getter];});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources),'timezone':'UTC'}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
