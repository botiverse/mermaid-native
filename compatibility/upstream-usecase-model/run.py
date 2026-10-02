#!/usr/bin/env python3
"""Replay unchanged Usecase production model assertions against the production Native syntax layer."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-boundary-mutations',action='store_true');o=p.parse_args()
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-usecase-model-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve(),r/'mermaid-layout-simple/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',r/'mermaid-layout-api/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar']));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'UsecaseModelNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
(w/'capture.ts').write_text("""import {vi,expect} from 'vitest';import {appendFileSync} from 'node:fs';
import {Diagram} from '../packages/mermaid/src/Diagram.js';
const original=Diagram.fromText;
vi.spyOn(Diagram,'fromText').mockImplementation(async (source,...args)=>{appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');return original.call(Diagram,source,...args);});
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-usecase-model-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-usecase-model-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-usecase-model-audit/{mode}.config.ts','--workspace',f'.native-usecase-model-audit/{mode}.workspace.ts','--testNamePattern','^.*when parsing (basic actors|use cases|relationships|system boundaries|direction|actor metadata|complex diagrams|class definitions)','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original Usecase production model suite failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==23
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'UsecaseModelNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True,timeout=60)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text("""import {vi} from 'vitest';import {readFileSync} from 'node:fs';
import {Diagram} from '../packages/mermaid/src/Diagram.js';
import {db} from '../packages/mermaid/src/diagrams/usecase/usecaseDb.js';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));let current;
vi.spyOn(Diagram,'fromText').mockImplementation(async source=>{current=models.get(source);if(!current)throw new Error('Missing Native model');if(current.error)throw new Error(current.error);return {type:current.diagramType};});
vi.spyOn(db,'clear').mockImplementation(()=>{current=undefined;});
for(const [method,key] of [['getActors','actors'],['getUseCases','useCases'],['getSystemBoundaries','boundaries']])vi.spyOn(db,method).mockImplementation(()=>new Map((current?.[key]??[]).map(x=>[x.id,x])));
vi.spyOn(db,'getSystemBoundary').mockImplementation(id=>current?.boundaries?.find(x=>x.id===id));
vi.spyOn(db,'getRelationships').mockImplementation(()=>current?.relationships??[]);
vi.spyOn(db,'getClassDefs').mockImplementation(()=>new Map(Object.entries(current?.classDefs??{})));
vi.spyOn(db,'getDirection').mockImplementation(()=>current?.direction);
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if status.returncode==0 and o.verify_boundary_mutations:
 original_cache=cache.read_text();mutations={}
 try:
  for field in ['type','members']:
   mutated=json.loads(original_cache);changed=0
   for _, model in mutated:
    for boundary in model.get('boundaries',[]):
     if boundary['id']=='sb1':
      boundary[field]='system' if field=='type' else [];changed+=1
   assert changed==1, 'Expected one boundary fixture'
   cache.write_text(json.dumps(mutated));mutation_status=test('native')
   result=json.loads((w/'native.json').read_text())
   failures=[a['fullName'] for suite in result['testResults'] for a in suite['assertionResults'] if a['status']=='failed']
   assert mutation_status.returncode!=0 and len(failures)==1 and 'explicit id' in failures[0], failures
   mutations[field]={'failedTests':failures,'passedTests':result['numPassedTests']}
   shutil.copy2(w/'native.json',w/f'mutation-{field}.json')
 finally:
  cache.write_text(original_cache)
  status=test('native')
 assert status.returncode==0, 'Restored Native assertions failed'
 summary['boundaryMutations']=mutations
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
