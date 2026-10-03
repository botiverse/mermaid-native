#!/usr/bin/env python3
"""Run unchanged usecaseDb assertions through actual Kotlin draft mutations/commit/getters."""
import argparse, hashlib, json, os, shutil, subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-usecase-document-audit';w.mkdir(exist_ok=True)
manifest=json.loads((h/'sources.json').read_text())
for f,sha in manifest['files'].items():
 for path in (u/f,r/'compatibility/upstream-batch/originals'/f):assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect,afterEach} from 'vitest';
import {db} from '../packages/mermaid/src/diagrams/usecase/usecaseDb.js';
const name=()=>expect.getState().currentTestName??'';
const ids=new WeakMap(),raws=new WeakMap();let counter=0;
const plain=x=>x===undefined?null:x instanceof Map?{$map:[...x].map(([k,v])=>[k,plain(v)])}:Array.isArray(x)?x.map(plain):x&&typeof x==='object'?Object.fromEntries(Object.entries(x).map(([k,v])=>[k,plain(v)])):x;
const unpack=x=>x&&typeof x==='object'?('$map' in x?new Map(x.$map.map(([k,v])=>[k,unpack(v)])):Array.isArray(x)?x.map(unpack):Object.fromEntries(Object.entries(x).map(([k,v])=>[k,unpack(v)]))):x;
const proxy=(value,id,path,call)=>{
 if(!value||typeof value!=='object')return value;
 const result=new Proxy(value,{
  get(target,key){
   if(target instanceof Map){
    if(key==='get')return k=>proxy(target.get(k),id,[...path,k],call);
    if(key==='set')return(k,v)=>{call('set',[id,[...path,k],plain(v)]);target.set(k,v);return result;};
    if(key==='clear')return()=>{call('empty',[id,path[0]]);target.clear();};
    const v=Reflect.get(target,key,target);return typeof v==='function'?v.bind(target):v;
   }
   const v=Reflect.get(target,key);return typeof key==='symbol'||typeof v==='function'?v:proxy(v,id,[...path,String(key)],call);
  },
  set(target,key,v){call('set',[id,[...path,String(key)],plain(v)]);return Reflect.set(target,key,v);}
 });
 raws.set(result,value);if(path.length===0)ids.set(result,id);return result;
};
const getters=['getActor','getActors','getUseCases','getSystemBoundaries','getRelationships','getNotes','getJsonNodes','getClassDefs','getDirection','getAST','getDiagramTitle','getAccTitle','getAccDescription','getConfig','getData'];
const setters=['setDiagramTitle','setAccTitle','setAccDescription'];
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
const call=(op,args=[])=>appendFileSync(CALLS,JSON.stringify({test:name(),op,args})+'\\n');
const create=db.createModel.bind(db),commit=db.commit.bind(db),clear=db.clear.bind(db);
vi.spyOn(db,'createModel').mockImplementation(()=>{const id=counter++;call('create',[id]);return proxy(create(),id,[],call);});
vi.spyOn(db,'commit').mockImplementation(model=>{const id=ids.get(model);if(id===undefined){if(model.notes!==undefined||model.actors.size||model.useCases.size||model.relationships.length)throw new Error('Unsupported incomplete import');call('incomplete',[]);}else call('commit',[id]);return commit(structuredClone(plainToNative(model)));});
// Remove only Proxy wrappers for the real reference commit; preserve Map semantics.
function plainToNative(x){return unpack(plain(x));}
vi.spyOn(db,'clear').mockImplementation(()=>{call('clear');return clear();});
for(const method of [...getters,...setters]){const original=db[method].bind(db);vi.spyOn(db,method).mockImplementation((...args)=>{call(method,args);return original(...args);});}
""".replace('CALLS',json.dumps(str(calls))))
# Undefined notes must remain undefined to exercise upstream complete-model validation.
pth=w/'capture.ts';pth.write_text(pth.read_text().replace("return commit(structuredClone(plainToNative(model)));","const value=raws.get(model)??plainToNative(model);if(model.notes===undefined)value.notes=undefined;return commit(value);"))
config="""import {defineConfig} from 'vitest/config';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';import {resolve} from 'node:path';export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-usecase-document-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(manifest['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-usecase-document-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-usecase-document-audit/{mode}.config.ts','--workspace',f'.native-usecase-document-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=180)
assert test('official').returncode==0,'Reference failed; see official.log'
assert json.loads((w/'official.json').read_text())['numPassedTests']==4
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'UsecaseDocumentBridge.java'),str(r/'compatibility/upstream-usecase-ast/UsecaseAstNativeBridge.java')],check=True)
trace=[json.loads(x) for x in calls.read_text().splitlines()]
res=subprocess.run(['java','-cp',cp,'UsecaseDocumentBridge'],input=calls.read_text(),text=True,capture_output=True,check=True,timeout=60)
snapshots=[json.loads(x) for x in res.stdout.splitlines()];assert len(trace)==len(snapshots)
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'snapshot':snap} for call,snap in zip(trace,snapshots)]))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8'));const byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
const call=(op,args=[])=>{const row=byTest.get(name())?.shift();if(!row||row.op!==op||JSON.stringify(row.args)!==JSON.stringify(args))throw new Error('Native trace mismatch '+name()+' '+op);if(row.snapshot.error)throw new Error(row.snapshot.error);return unpack(row.snapshot.value);};
afterEach(()=>{if(byTest.get(name())?.length)throw new Error('Unconsumed Native operations: '+name());});
vi.spyOn(db,'createModel').mockImplementation(()=>{const id=counter++;const value=call('create',[id]);delete value.$draftId;return proxy(value,id,[],call);});
vi.spyOn(db,'commit').mockImplementation(model=>{const id=ids.get(model);return id===undefined?call('incomplete',[]):call('commit',[id]);});
vi.spyOn(db,'clear').mockImplementation(()=>call('clear'));
for(const method of [...getters,...setters])vi.spyOn(db,method).mockImplementation((...args)=>{const value=call(method,args);return (method==='getAST'||method==='getActor')&&value===null?undefined:value;});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName'] for s in json.loads((w/'native.json').read_text())['testResults'] for a in s['assertionResults'] if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':manifest['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'nativeOperations':len(trace)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations:
 original=cache.read_text();controls={}
 try:
  for key in ['detachment','replacement','incomplete','reset','padding']:
   rows=json.loads(original);targets=set()
   for row in rows:
    value=row['snapshot'].get('value');op=row['op'];name=row['test']
    if key=='detachment' and op=='getActor':value['label']='mutated draft'
    elif key=='replacement' and op=='getActors' and 'atomically replaces' in name:row['snapshot']['value']={'$map':[['User',{'id':'User'}]]}
    elif key=='incomplete' and op=='incomplete':row['snapshot']={'value':None}
    elif key=='reset' and op=='getDirection':row['snapshot']['value']='RL'
    elif key=='padding' and op=='getData':value['nodes'][0]['padding']=10
    else:continue
    targets.add(name.replace(' > ',' '))
   assert len(targets)==1 and not targets&baseline,(key,targets,baseline)
   cache.write_text(json.dumps(rows));test('native');assert failed()-baseline==targets,(key,failed(),targets)
   controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:
  cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(w/'failures.json').write_text(json.dumps(sorted(baseline),indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
