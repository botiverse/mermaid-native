#!/usr/bin/env python3
"""Replay unchanged Requirement model assertions through production Kotlin parsing and styles."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--stdlib',type=Path,required=True);o=p.parse_args()
u=o.upstream.resolve();r=o.repo.resolve();h=Path(__file__).resolve().parent;w=u/'.native-requirement-model-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar)
cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]));subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'RequirementModelNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect} from 'vitest';
import {RequirementDB} from '../packages/mermaid/src/diagrams/requirement/requirementDb.js';
const sources=new WeakMap();
const methods=['addRequirement','addElement','addRelationship','defineClass','setCssStyle','setClass','setDirection'];
function append(db,method,args){
 const lines=sources.get(db)||['requirementDiagram'];let line='';
 if(method==='addRequirement')line=args[1].toLowerCase()+' '+JSON.stringify(args[0])+' {\\n}';
 if(method==='addElement')line='element '+JSON.stringify(args[0])+' {\\n}';
 if(method==='addRelationship')line=JSON.stringify(args[1])+' - '+args[0]+' -> '+JSON.stringify(args[2]);
 if(method==='defineClass')line='classDef '+args[0].join(',')+' '+args[1].join(',');
 if(method==='setCssStyle')line='style '+args[0].join(',')+' '+args[1].join(',');
 if(method==='setClass')line='class '+args[0].join(',')+' '+args[1].join(',');
 if(method==='setDirection')line='direction '+args[0];
 return [...lines,line];
}
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
function record(source){appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');}
const clear=RequirementDB.prototype.clear;
vi.spyOn(RequirementDB.prototype,'clear').mockImplementation(function(){sources.set(this,['requirementDiagram']);record('requirementDiagram');return clear.call(this);});
for(const method of methods){const original=RequirementDB.prototype[method];vi.spyOn(RequirementDB.prototype,method).mockImplementation(function(...args){const lines=append(this,method,args);record(lines.join('\\n'));const result=original.apply(this,args);sources.set(this,lines);return result;});}
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-requirement-model-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-requirement-model-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-requirement-model-audit/{mode}.config.ts','--workspace',f'.native-requirement-model-audit/{mode}.workspace.ts','--testNamePattern','^(?!.*unique edge ids).*$','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,env={**os.environ,'TZ':'UTC'},stdout=f,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original Requirement model suite failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==9,'Expected exactly 9 selected original assertions'
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'RequirementModelNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True,timeout=60)
models=[json.loads(x) for x in result.stdout.splitlines()];assert len(models)==len(sources)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(sources,models))))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));const state=new WeakMap();
function lookup(source){if(!models.has(source))throw new Error('Missing Native result');const result=models.get(source);if(result.error)throw new Error(result.error);return structuredClone(result);}
vi.spyOn(RequirementDB.prototype,'clear').mockImplementation(function(){sources.set(this,['requirementDiagram']);state.set(this,lookup('requirementDiagram'));});
for(const method of methods)vi.spyOn(RequirementDB.prototype,method).mockImplementation(function(...args){const lines=append(this,method,args);state.set(this,lookup(lines.join('\\n')));sources.set(this,lines);});
vi.spyOn(RequirementDB.prototype,'getRequirements').mockImplementation(function(){return new Map(state.get(this).requirements.map(n=>[n.name,n]));});
vi.spyOn(RequirementDB.prototype,'getElements').mockImplementation(function(){return new Map(state.get(this).elements.map(n=>[n.name,n]));});
vi.spyOn(RequirementDB.prototype,'getRelationships').mockImplementation(function(){return state.get(this).edges;});
vi.spyOn(RequirementDB.prototype,'getClasses').mockImplementation(function(){return new Map(Object.entries(state.get(this).definitions).map(([id,styles])=>[id,{id,styles}]));});
vi.spyOn(RequirementDB.prototype,'getDirection').mockImplementation(function(){return state.get(this).direction;});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
