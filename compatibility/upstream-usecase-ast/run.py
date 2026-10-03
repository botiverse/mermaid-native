#!/usr/bin/env python3
"""Run the complete unchanged parser-publication file against a real Native document session."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
r=Path(__file__).resolve().parents[2];h=Path(__file__).resolve().parent;u=o.upstream.resolve();w=u/'.native-usecase-ast-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
calls=w/'official-calls.jsonl';calls.write_text('')
(w/'capture.ts').write_text("""import {vi,expect} from 'vitest';import {appendFileSync} from 'node:fs';
import {db} from '../packages/mermaid/src/diagrams/usecase/usecaseDb.js';
import {parser} from '../packages/mermaid/src/diagrams/usecase/parser/usecase.chevrotain.js';
let parsing=false;const save=(op,source)=>appendFileSync(CALLS,JSON.stringify({op,source,test:expect.getState().currentTestName??''})+'\\n');
const originalClear=db.clear;vi.spyOn(db,'clear').mockImplementation(()=>{if(!parsing)save('clear');return originalClear.call(db);});
const originalParse=parser.parse;vi.spyOn(parser,'parse').mockImplementation(async source=>{save('parse',source);parsing=true;try{return await originalParse.call(parser,source);}finally{parsing=false;}});
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-usecase-ast-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-usecase-ast-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-usecase-ast-audit/{mode}.config.ts','--workspace',f'.native-usecase-ast-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed'
assert json.loads((w/'official.json').read_text())['numPassedTests']==77
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar);cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'UsecaseAstNativeBridge.java')],check=True)
trace=[json.loads(x) for x in calls.read_text().splitlines()];commands=['C' if x['op']=='clear' else 'P:'+base64.b64encode(x['source'].encode()).decode() for x in trace]
result=subprocess.run(['java','-cp',cp,'UsecaseAstNativeBridge'],input='\n'.join(commands)+'\n',text=True,capture_output=True,check=True,timeout=60);snapshots=[json.loads(x) for x in result.stdout.splitlines()];assert len(snapshots)==len(trace)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'snapshot':snapshot} for call,snapshot in zip(trace,snapshots)]))
(w/'adapter.ts').write_text("""import {vi,expect} from 'vitest';import {readFileSync} from 'node:fs';
import {db} from '../packages/mermaid/src/diagrams/usecase/usecaseDb.js';
import * as common from '../packages/mermaid/src/diagrams/common/commonDb.js';
import {parser} from '../packages/mermaid/src/diagrams/usecase/parser/usecase.chevrotain.js';
const trace=JSON.parse(readFileSync(CACHE,'utf8'));const byTest=new Map();for(const row of trace){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}let current;
const step=(op,source)=>{const name=expect.getState().currentTestName??'';const next=byTest.get(name)?.shift();if(!next||next.op!==op||next.source!==source)throw new Error('Native operation trace mismatch: '+name+' '+op);current=next.snapshot;return current;};
vi.spyOn(db,'clear').mockImplementation(()=>{step('clear');});
vi.spyOn(parser,'parse').mockImplementation(async source=>{const next=step('parse',source);if(next.error)throw new Error(next.error);});
for(const [method,key] of [['getActors','actors'],['getUseCases','useCases'],['getSystemBoundaries','boundaries'],['getJsonNodes','jsonNodes'],['getNotes','notes']])vi.spyOn(db,method).mockImplementation(()=>new Map((current?.[key]??[]).map(x=>[x.id,x])));
vi.spyOn(db,'getRelationships').mockImplementation(()=>current?.relationships??[]);
vi.spyOn(db,'getClassDefs').mockImplementation(()=>new Map(Object.entries(current?.classDefs??{})));
for(const [method,key] of [['getActor','actors'],['getUseCase','useCases'],['getSystemBoundary','boundaries']])vi.spyOn(db,method).mockImplementation(id=>current?.[key]?.find(x=>x.id===id));
vi.spyOn(db,'getDirection').mockImplementation(()=>current?.direction);
vi.spyOn(db,'getAST').mockImplementation(()=>current?.ast);
vi.spyOn(common,'getAccTitle').mockImplementation(()=>current?.accTitle);
vi.spyOn(common,'getAccDescription').mockImplementation(()=>current?.accDescr);
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName'] for s in json.loads((w/'native.json').read_text())['testResults'] for a in s['assertionResults'] if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':m['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'nativeOperations':len(trace),'nativeParseCalls':sum(c['op']=='parse' for c in trace),'nativeClearCalls':sum(c['op']=='clear' for c in trace)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations:
 original=cache.read_text();controls={}
 try:
  for key in ['headerSpan','jsonIdSpan','failedPublication','collisionOrigin','unquotedToken','grammarLocation','metadataLocation','relationKind','classTarget','noteTarget','jsonRelation','edgeAnimation','astAnimation','astJson']:
   mutated=json.loads(original);targets=set();changed=0
   for row in mutated:
    source=row.get('source','');snap=row['snapshot']
    if key=='headerSpan' and source=='usecase-beta':snap['ast']['header']['span'][1]+=1
    elif key=='jsonIdSpan' and source=='usecase-beta\njson son @{"a": 1}':snap['ast']['statements'][-1]['nodes'][0]['idSpan'][0]-=1
    elif key=='failedPublication' and 'actor Draft' in source and 'note for Missing "invalid"' in source:snap['ast']={'stale':True}
    elif key=='collisionOrigin' and source=='usecase-beta\n\"A-B\"\n\"A B\"' and 'names both exact locations' in row['test']:snap['error']=snap['error'].replace('previous declaration at', 'missing previous declaration at')
    elif key=='unquotedToken' and source=='usecase-beta\nLiteral(Literal {nested} markers)':snap['error']=snap['error'].replace('but found:', 'token:')
    elif key=='grammarLocation' and "locates rejected 'JSON' content" in row['test'] and source=='usecase-beta\nsystemBoundary Auth\njson Payload@{}\nend':snap['error']=snap['error'].replace('at line', 'at missing line')
    elif key=='metadataLocation' and source=='usecase-beta\nactor User@{ type: giant }':snap['error']=snap['error'].replace('column', 'missing column')
    elif key=='relationKind' and source=='usecase-beta\nactor User\nLogin\nUser --|> Login':snap['error']=snap['error'].replace('Generalization', 'Association')
    elif key=='classTarget' and source=='usecase-beta\nA known@--> B\nclass missingEdge decorated':snap['error']=snap['error'].replace('unresolved or anonymous', 'accepted')
    elif key=='noteTarget' and source=='usecase-beta\njson Payload@{}\nnote for Payload \"invalid\"':snap['error']=snap['error'].replace('previous declaration at', 'missing origin at')
    elif key=='jsonRelation' and source=='usecase-beta\njson Payload@{}\nInspect --o Payload':snap['error']=snap['error'].replace('only point', 'any point')
    elif key=='edgeAnimation' and 'A trueEdge@--> B' in source:snap['relationships'][1]['animate']=False
    elif key=='astAnimation' and 'A trueEdge@--> B' in source:snap['ast']['edges'][2]['attrs']['animation']='fast'
    elif key=='astJson' and '%% representative' in source:snap['ast']['nodes']['Payload']['attrs']['value']['ok']=False
    else:continue
    targets.add(row['test'].replace(' > ',' '));changed+=1
   assert changed==1 and len(targets)==1 and not(targets&baseline),(key,targets,baseline)
   cache.write_text(json.dumps(mutated));test('native');delta=failed()-baseline;assert delta==targets,(key,delta,targets);assert baseline<=failed();controls[key]=sorted(delta);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
