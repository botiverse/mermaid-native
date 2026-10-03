#!/usr/bin/env python3
"""Replay unchanged Usecase production model assertions against the production Native syntax layer."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-model-mutations','--verify-boundary-mutations',dest='verify_model_mutations',action='store_true');o=p.parse_args()
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-usecase-model-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve(),r/'mermaid-layout-simple/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',r/'mermaid-layout-api/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar']));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'UsecaseModelNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
(w/'capture.ts').write_text("""import {vi,expect} from 'vitest';import {appendFileSync} from 'node:fs';
import {Diagram} from '../packages/mermaid/src/Diagram.js';
import {parser} from '../packages/mermaid/src/diagrams/usecase/parser/usecase.chevrotain.js';
const originalParse=parser.parse;
vi.spyOn(parser,'parse').mockImplementation(async (source,...args)=>{
 if(expect.getState().currentTestName?.startsWith('usecase parser publication'))appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');
 return originalParse.call(parser,source,...args);
});
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
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-usecase-model-audit/{mode}.config.ts','--workspace',f'.native-usecase-model-audit/{mode}.workspace.ts','--testNamePattern','^.*when parsing (basic actors|use cases|relationships|system boundaries|direction|actor metadata|complex diagrams|class definitions)|^usecase parser publication.*(accepts ids that start with a digit|keeps decimal style values intact now that ids may start with a digit|lets a later metadata statement override inline boundary metadata|preserves the separators inside multi-word style and classDef values|resolves forward .* refinement independently of declaration order)$|database methods should get specific (actor|use case) by id$','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original Usecase production model suite failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==31
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'UsecaseModelNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True,timeout=60)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text("""import {vi} from 'vitest';import {readFileSync} from 'node:fs';
import {Diagram} from '../packages/mermaid/src/Diagram.js';
import {db} from '../packages/mermaid/src/diagrams/usecase/usecaseDb.js';
import {parser} from '../packages/mermaid/src/diagrams/usecase/parser/usecase.chevrotain.js';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));let current;
vi.spyOn(parser,'parse').mockImplementation(async source=>{current=models.get(source);if(!current)throw new Error('Missing Native model');if(current.error)throw new Error(current.error);});
vi.spyOn(Diagram,'fromText').mockImplementation(async source=>{current=models.get(source);if(!current)throw new Error('Missing Native model');if(current.error)throw new Error(current.error);return {type:current.diagramType};});
vi.spyOn(db,'clear').mockImplementation(()=>{current=undefined;});
for(const [method,key] of [['getActors','actors'],['getUseCases','useCases'],['getSystemBoundaries','boundaries'],['getJsonNodes','jsonNodes']])vi.spyOn(db,method).mockImplementation(()=>new Map((current?.[key]??[]).map(x=>[x.id,x])));
vi.spyOn(db,'getUseCase').mockImplementation(id=>current?.useCases?.find(x=>x.id===id));
vi.spyOn(db,'getActor').mockImplementation(id=>current?.actors?.find(x=>x.id===id));
vi.spyOn(db,'getSystemBoundary').mockImplementation(id=>current?.boundaries?.find(x=>x.id===id));
vi.spyOn(db,'getRelationships').mockImplementation(()=>current?.relationships??[]);
vi.spyOn(db,'getClassDefs').mockImplementation(()=>new Map(Object.entries(current?.classDefs??{})));
vi.spyOn(db,'getNotes').mockImplementation(()=>{
 if(current?.noteCount!==0)throw new Error('Native note ID projection is not covered by this adapter');
 return new Map();
});
vi.spyOn(db,'getDirection').mockImplementation(()=>current?.direction);
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if status.returncode==0 and o.verify_model_mutations:
 original_cache=cache.read_text();mutations={}
 try:
  for field in ['type','members','numericActorId','decimalStyles','overrideType','classWordSeparators','forwardActor','forwardRectangle']:
   mutated=json.loads(original_cache);changed=0
   for source, model in mutated:
    if field=='forwardActor' and source=='usecase-beta\nUser --> Login\nactor User':
     model['actors']=[];changed+=1
    elif field=='forwardRectangle' and source=='usecase-beta\nUser --> Login\nLogin[Sign in]':
     for node in model.get('useCases',[]):
      if node['id']=='Login':node['shape']='ellipse';changed+=1
    elif field=='overrideType':
     for boundary in model.get('boundaries',[]):
      if boundary['id']=='Payment_service' and boundary.get('type')=='rect':boundary['type']='package';changed+=1
    elif field=='classWordSeparators':
     definition=model.get('classDefs',{}).get('big')
     if definition and definition['styles']==['font-family:Arial Black']:definition['styles']=['font-family:ArialBlack'];changed+=1
    elif field=='numericActorId':
     for actor in model.get('actors',[]):
      if actor['id']=='1mg':actor['id']='mg';changed+=1
    elif field=='decimalStyles':
     for node in model.get('useCases',[]):
      if node.get('styles')==['stroke-width:1.5px','opacity:0.5','margin:2']:node['styles']=['stroke-width:1px','opacity:0','margin:2'];changed+=1
    for boundary in model.get('boundaries',[]) if field in ['type','members'] else []:
     if boundary['id']=='sb1':
      boundary[field]='system' if field=='type' else [];changed+=1
   assert changed==1, 'Expected one model fixture'
   cache.write_text(json.dumps(mutated));mutation_status=test('native')
   result=json.loads((w/'native.json').read_text())
   failures=[a['fullName'] for suite in result['testResults'] for a in suite['assertionResults'] if a['status']=='failed']
   expected='accepts ids that start with a digit' if field=='numericActorId' else 'keeps decimal style values' if field=='decimalStyles' else 'lets a later metadata statement override' if field=='overrideType' else 'preserves the separators' if field=='classWordSeparators' else "resolves forward 'actor' refinement" if field=='forwardActor' else "resolves forward 'rectangular use case' refinement" if field=='forwardRectangle' else 'explicit id'
   assert mutation_status.returncode!=0 and len(failures)==1 and expected in failures[0], failures
   mutations[field]={'failedTests':failures,'passedTests':result['numPassedTests']}
   shutil.copy2(w/'native.json',w/f'mutation-{field}.json')
 finally:
  cache.write_text(original_cache)
  status=test('native')
 assert status.returncode==0, 'Restored Native assertions failed'
 summary['modelMutations']=mutations
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
