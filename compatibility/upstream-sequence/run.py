#!/usr/bin/env python3
"""Run unmodified upstream Sequence assertions with the official and Kotlin parsers."""
import argparse, base64, hashlib, json, os, subprocess
from pathlib import Path

args = argparse.ArgumentParser(description=__doc__)
args.add_argument('--upstream', required=True, type=Path)
args.add_argument('--repo', default=Path(__file__).resolve().parents[2], type=Path)
args.add_argument('--skip-build', action='store_true')
args.add_argument('--stdlib', required=True, type=Path, help='Kotlin 2.1.21 stdlib JAR')
opt = args.parse_args()
here = Path(__file__).resolve().parent
repo = opt.repo.resolve()
upstream = opt.upstream.resolve()
work = upstream / '.native-sequence-audit'
work.mkdir(exist_ok=True)
manifest = json.loads((here / 'sources.json').read_text())
for file, expected in manifest['files'].items():
    actual = hashlib.sha256((upstream / file).read_bytes()).hexdigest()
    if actual != expected:
        raise SystemExit(f'Upstream source differs from pinned revision: {file}')

def run(argv, cwd=upstream, log=None, check=True):
    with open(log or os.devnull, 'w') as output:
        return subprocess.run(argv, cwd=cwd, stdout=output, stderr=subprocess.STDOUT, check=check)

if not opt.skip_build: run([str(repo / 'gradlew'), ':mermaid-core:bundleLibRuntimeToJarDebug'], repo, work / 'native-build.log')
jar = repo / 'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar'
cp = os.pathsep.join(map(str, [work, jar, opt.stdlib.resolve()]))
run(['javac', '-cp', cp, '-d', str(work), str(here / 'SequenceNativeBridge.java')])
fixtures = ['packages/mermaid/src/diagrams/sequence/sequenceDiagram.spec.js']
captured = work / 'official-calls.jsonl'
captured.write_text('')
(work / 'capture.ts').write_text('''
import {appendFileSync} from 'node:fs';
import {beforeAll,afterAll,expect} from 'vitest';
import erDiagram from '../packages/mermaid/src/diagrams/sequence/parser/sequenceDiagram.jison';
let original;
beforeAll(()=>{original=erDiagram.parser.parse;erDiagram.parser.parse=function(source){
 appendFileSync(CAPTURE_PATH,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');
 return original.call(this,source);
};});
afterAll(()=>{erDiagram.parser.parse=original;});
'''.replace('CAPTURE_PATH', json.dumps(str(captured))))
config = '''
import {defineConfig} from 'vitest/config';
import jison from '../.vite/jisonPlugin.js';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({
 plugins:[PARSER_PLUGIN,jsonSchemaPlugin()],
 test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FIXTURE,SETUP},
 define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"}
});
'''.replace('FIXTURE', json.dumps(fixtures))
(work / 'official.config.ts').write_text(config.replace('PARSER_PLUGIN','jison()').replace('SETUP',"setupFiles:['.native-sequence-audit/capture.ts']"))
(work / 'native.config.ts').write_text(config.replace('PARSER_PLUGIN',"{name:'native-sequence',enforce:'pre',resolveId(id){if(id.endsWith('sequenceDiagram.jison'))return resolve('.native-sequence-audit/adapter.ts');}}").replace('SETUP',''))
for mode in ['official','native']:
    (work / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-sequence-audit/"+mode+".config.ts']);\n")

def test(mode):
    return run(['pnpm','exec','vitest','run','--config',f'.native-sequence-audit/{mode}.config.ts','--workspace',f'.native-sequence-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={work / (mode+".json")}'],log=work / f'{mode}.log',check=False)

if test('official').returncode:
    raise SystemExit(f'Official reference suite failed; inspect {work / "official.log"}')
sources = list(dict.fromkeys(json.loads(line)['source'] for line in captured.read_text().splitlines()))
result = subprocess.run(['java','-cp',cp,'SequenceNativeBridge','--batch'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True)
models = [json.loads(line) for line in result.stdout.splitlines()]
assert len(models) == len(sources)
cache = work / 'native-models.json'
cache.write_text(json.dumps(list(zip(sources,models))))
(work / 'adapter.ts').write_text("""
import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE_PATH,'utf8')));
const parser={yy:null,parse(source){
 const model=models.get(source);if(!model)throw new Error('Missing Native result');if(model.error)throw new Error(model.error);
 const db=this.yy;

 if(model.title!==null)db.setDiagramTitle(model.title);
 if(model.accTitle!==null)db.setAccTitle(model.accTitle);
 if(model.accDescription!==null)db.setAccDescription(model.accDescription);
 const label=(text,wrap)=>({text,wrap:wrap??undefined});
 for(const a of model.actors)db.addActor(a.id,a.id,label(a.label,a.wrap),a.kind.toLowerCase());
 for(const e of model.events){
  if(e.kind==='message'){
   let type=e.bidirectional?'BIDIRECTIONAL_SOLID':{FILLED:'SOLID',NONE:'SOLID_OPEN',OPEN:'SOLID_POINT',CROSS:'SOLID_CROSS',CIRCLE:'SOLID_POINT'}[e.head];
   if(e.head.startsWith('HALF_')){
    const filled=e.head.includes('FILLED'),side=e.head.endsWith('TOP')?'TOP':'BOTTOM';
    type=(filled?'SOLID':'STICK')+(e.headAtSource?'_ARROW':'')+'_'+side+(e.headAtSource?'_REVERSE':'');
    if(e.style==='DASHED')type+='_DOTTED';
   } else if(e.style==='DASHED')type=type.replace('SOLID','DOTTED');
   db.addSignal(e.from,e.to,label(e.label,e.wrap),db.LINETYPE[type],e.activate,{NONE:0,TO:59,FROM:60,BOTH:61}[e.central]);
   if(e.central==='TO'||e.central==='BOTH')db.addSignal(e.to,undefined,undefined,db.LINETYPE.CENTRAL_CONNECTION);
   if(e.central==='FROM'||e.central==='BOTH')db.addSignal(e.from,undefined,undefined,db.LINETYPE.CENTRAL_CONNECTION_REVERSE);
  } else if(e.kind==='note'){
   const actors=e.position==='OVER'?(e.actors.length===1?[e.actors[0],e.actors[0]]:e.actors):e.actors[0];
   db.addNote(actors,db.PLACEMENT[{LEFT_OF:'LEFTOF',RIGHT_OF:'RIGHTOF',OVER:'OVER'}[e.position]],label(e.text,e.wrap));
  } else if(e.kind==='activation')db.addSignal(e.actor,undefined,undefined,db.LINETYPE[e.activate?'ACTIVE_START':'ACTIVE_END']);
  else if(e.kind==='numbering')db.apply({type:'sequenceIndex',sequenceIndex:e.start??undefined,sequenceIndexStep:e.step??undefined,sequenceVisible:e.visible,signalType:db.LINETYPE.AUTONUMBER});
  else {
   let key=e.fragment+'_'+(e.boundary==='BRANCH'?{ALT:'ELSE',PAR:'AND',PAR_OVER:'AND',CRITICAL:'OPTION'}[e.fragment]:e.boundary);
   if(key==='PAR_OVER_END')key='PAR_END';if(key==='PAR_OVER_AND')key='PAR_AND';
   db.addSignal(undefined,undefined,e.boundary==='END'?undefined:label(e.label,e.wrap),db.LINETYPE[key]);
  }
 }
 return true;
}};
export default {parser,parse(source){return parser.parse(source)}};
""".replace('CACHE_PATH',json.dumps(str(cache))))
status = test('native')
summary = {'upstreamRevision':manifest['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
    report = json.loads((work / f'{mode}.json').read_text())
    summary[mode] = {k:report[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(work / 'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2))
raise SystemExit(status.returncode)
