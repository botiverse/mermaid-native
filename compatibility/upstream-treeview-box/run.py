#!/usr/bin/env python3
"""Execute unchanged original TreeView preprocessor and product-consumer assertions."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-treeview-box-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'TreeViewBoxNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
imports="""import {vi,afterAll,expect} from 'vitest';
import * as box from '../packages/mermaid/src/diagrams/treeView/boxDrawingPreprocessor.js';
import {parser} from '../packages/mermaid/src/diagrams/treeView/parser.js';
import db from '../packages/mermaid/src/diagrams/treeView/db.js';
const key=(method,args)=>JSON.stringify([method,args],(_,v)=>v instanceof Map?{map:[...v]}:v);
"""
(w/'capture.ts').write_text(imports+"""import {appendFileSync} from 'node:fs';
for(const [method,object,name] of [['detect',box,'isBoxDrawingFormat'],['preprocess',box,'preprocessBoxDrawing'],['remap',box,'remapErrorLines'],['parse',parser,'parse']]){
 const original=object[name];vi.spyOn(object,name).mockImplementation(function(...args){appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,key:key(method,args)})+'\\n');return Reflect.apply(original,this,args);});
}
afterAll(()=>vi.restoreAllMocks());
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-treeview-box-audit/SETUP.ts']}});""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-treeview-box-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-treeview-box-audit/{mode}.config.ts','--workspace',f'.native-treeview-box-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT)
if test('official').returncode:raise SystemExit('Original TreeView suite failed')
keys=list(dict.fromkeys(json.loads(x)['key'] for x in calls.read_text().splitlines()));requests=[]
for key in keys:
 method,args=json.loads(key);source='\n'.join(args[0]) if method=='detect' else args[0];mapping=','.join(f'{a}:{b}' for a,b in args[1]['map']) if method=='remap' else ''
 requests.append(method+'\t'+base64.b64encode(source.encode()).decode()+'\t'+mapping)
result=subprocess.run(['java','-cp',cp,'TreeViewBoxNativeBridge'],input='\n'.join(requests)+'\n',text=True,capture_output=True,check=True)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(keys)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(keys,models))))
(w/'adapter.ts').write_text(imports+"""import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
function invoke(method,args){const result=models.get(key(method,args));if(!result)throw new Error('Missing Native result');if(result.error)throw new Error(result.error);return structuredClone(result.value);}
vi.spyOn(box,'isBoxDrawingFormat').mockImplementation((...args)=>invoke('detect',args));
vi.spyOn(box,'preprocessBoxDrawing').mockImplementation((...args)=>{const result=invoke('preprocess',args);result.lineMap=new Map(result.lineMap);return result;});
vi.spyOn(box,'remapErrorLines').mockImplementation((...args)=>invoke('remap',args));
let root;vi.spyOn(db,'clear').mockImplementation(()=>{root=undefined;});vi.spyOn(db,'getRoot').mockImplementation(()=>root);
vi.spyOn(parser,'parse').mockImplementation(async(...args)=>{root=invoke('parse',args);});
afterAll(()=>vi.restoreAllMocks());
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueCalls':len(keys)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
