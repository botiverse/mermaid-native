#!/usr/bin/env python3
"""Execute unchanged upstream Pie parser assertions against Kotlin model values."""
import argparse,base64,hashlib,json,os,subprocess
from pathlib import Path
args=argparse.ArgumentParser(description=__doc__)
args.add_argument('--upstream',required=True,type=Path)
args.add_argument('--stdlib',required=True,type=Path)
args.add_argument('--repo',type=Path)
args.add_argument('--skip-build',action='store_true')
a=args.parse_args();here=Path(__file__).resolve().parent;repo=(a.repo or here.parents[1]).resolve();upstream=a.upstream.resolve();work=upstream/'.native-pie-audit';work.mkdir(exist_ok=True)
manifest=json.loads((here/'sources.json').read_text())
for f,sha in manifest['files'].items():
 if hashlib.sha256((upstream/f).read_bytes()).hexdigest()!=sha:raise SystemExit('Pinned source differs: '+f)
def run(argv,log,cwd=upstream,check=True):
 with open(log,'w') as out:return subprocess.run(argv,cwd=cwd,stdout=out,stderr=subprocess.STDOUT,check=check)
if not a.skip_build:run([str(repo/'gradlew'),':mermaid-core:bundleLibRuntimeToJarDebug'],work/'native-build.log',repo)
jar=repo/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar';cp=os.pathsep.join(map(str,[work,jar,a.stdlib.resolve()]))
run(['javac','-cp',cp,'-d',str(work),str(here/'PieNativeBridge.java')],work/'javac.log')
captured=work/'calls.jsonl';captured.write_text('')
fixtures=['packages/mermaid/src/diagrams/pie/pie.spec.ts','packages/parser/tests/pie.test.ts']
for mode in ['official','native']:
 (work/f'{mode}.config.ts').write_text('''import {defineConfig} from 'vitest/config';
import jison from '../.vite/jisonPlugin.js';import schema from '../.vite/jsonSchemaPlugin.js';
export default defineConfig({plugins:[jison(),schema()],test:{environment:'jsdom',globals:true,maxWorkers:1,include:FIXTURES,testNamePattern:'pie (parse |should handle)',setupFiles:['.native-pie-audit/MODE.setup.ts']},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"}});
'''.replace('FIXTURES',json.dumps(fixtures)).replace('MODE',mode))
 (work/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-pie-audit/"+mode+".config.ts']);")
 (work/f'{mode}.setup.ts').write_text('''import {vi} from 'vitest';
vi.mock('../packages/parser/tests/test-util.js',async(importOriginal)=>{
 const actual=await importOriginal();const fs=await import('node:fs');
 const models=IS_NATIVE?new Map(JSON.parse(fs.readFileSync(CACHE,'utf8'))):null;
 return {...actual,pieParse(source){
  if(!IS_NATIVE){fs.appendFileSync(CAPTURE,JSON.stringify(source)+'\\n');return actual.pieParse(source);}
  const value=models.get(source);if(!value)throw new Error('Missing Native result');
  return value.error?{value:{},lexerErrors:[],parserErrors:[{message:value.error}]}:{value,lexerErrors:[],parserErrors:[]};
 }};
});
vi.mock('../packages/mermaid/src/diagrams/pie/pieParser.js',async(importOriginal)=>{
 const fs=await import('node:fs');
 const models=IS_NATIVE?new Map(JSON.parse(fs.readFileSync(CACHE,'utf8'))):null;
 const actual=IS_NATIVE?null:await importOriginal();
 const {db}=await import('../packages/mermaid/src/diagrams/pie/pieDb.js');
 return {parser:{async parse(source){
  if(!IS_NATIVE){fs.appendFileSync(CAPTURE,JSON.stringify(source)+'\\n');return actual.parser.parse(source);}
  const model=models.get(source);if(!model)throw new Error('Missing Native result');if(model.error)throw new Error(model.error);
  if(model.title!==null)db.setDiagramTitle(model.title);
  if(model.accTitle!==null)db.setAccTitle(model.accTitle);
  if(model.accDescr!==null)db.setAccDescription(model.accDescr);
  db.setShowData(model.showData);model.sections.forEach(section=>db.addSection(section));
 }}};
});
'''.replace('IS_NATIVE','true' if mode=='native' else 'false').replace('CACHE',json.dumps(str(work/'models.json'))).replace('CAPTURE',json.dumps(str(captured))))
def test(mode):return run(['pnpm','exec','vitest','run','--config',f'.native-pie-audit/{mode}.config.ts','--workspace',f'.native-pie-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={work/(mode+".json")}'],work/f'{mode}.log',check=False)
if test('official').returncode:raise SystemExit('Reference failed: '+str(work/'official.log'))
sources=list(dict.fromkeys(json.loads(l) for l in captured.read_text().splitlines()))
result=subprocess.run(['java','-cp',cp,'PieNativeBridge'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True)
models=[json.loads(l) for l in result.stdout.splitlines()];assert len(models)==len(sources)
(work/'models.json').write_text(json.dumps(list(zip(sources,models))))
status=test('native');summary={'revision':manifest['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources),'excluded':'3 configuration cases (including 2 upstream todo) do not exercise Native parsing'}
for mode in ['official','native']:
 report=json.loads((work/f'{mode}.json').read_text());summary[mode]={k:report[k] for k in ['numPassedTests','numFailedTests','numPendingTests','numTodoTests','success']}
(work/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
