#!/usr/bin/env python3
"""Run unmodified upstream Flow assertions with the official and Kotlin parsers."""
import argparse, base64, hashlib, json, os, subprocess, shutil
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
work = upstream / '.native-flow-audit'
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
snapshot = work / 'native-runtime.jar'
shutil.copyfile(jar, snapshot)
cp = os.pathsep.join(map(str, [work, snapshot, opt.stdlib.resolve()]))
run(['javac', '-cp', cp, '-d', str(work), str(here / 'FlowNativeBridge.java')])
fixtures = sorted(str(f.relative_to(upstream)) for f in (upstream / 'packages/mermaid/src/diagrams/flowchart/parser').glob('*.spec.js'))
captured = work / 'official-calls.jsonl'
captured.write_text('')
(work / 'capture.ts').write_text('''
import {appendFileSync} from 'node:fs';
import {beforeAll,afterAll,expect} from 'vitest';
import erDiagram from '../packages/mermaid/src/diagrams/flowchart/parser/flowParser.ts';
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
(work / 'official.config.ts').write_text(config.replace('PARSER_PLUGIN','jison()').replace('SETUP',"setupFiles:['.native-flow-audit/capture.ts']"))
(work / 'native.config.ts').write_text(config.replace('PARSER_PLUGIN',"{name:'native-flow',enforce:'pre',resolveId(id){if(id.endsWith('flowParser.ts'))return resolve('.native-flow-audit/adapter.ts');}}").replace('SETUP',''))
for mode in ['official','native']:
    (work / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-flow-audit/"+mode+".config.ts']);\n")

def test(mode):
    return run(['pnpm','exec','vitest','run','--config',f'.native-flow-audit/{mode}.config.ts','--workspace',f'.native-flow-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={work / (mode+".json")}'],log=work / f'{mode}.log',check=False)

if test('official').returncode:
    raise SystemExit(f'Official reference suite failed; inspect {work / "official.log"}')
sources = list(dict.fromkeys(json.loads(line)['source'] for line in captured.read_text().splitlines()))
result = subprocess.run(['java','-cp',cp,'FlowNativeBridge','--batch'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True)
models = [json.loads(line) for line in result.stdout.splitlines()]
assert len(models) == len(sources)
cache = work / 'native-models.json'
cache.write_text(json.dumps(list(zip(sources,models))))
(work / 'adapter.ts').write_text("""
import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE_PATH,'utf8')));
export const parser={yy:null,parse(source){
 const model=models.get(source);if(!model)throw new Error('Missing Native result');if(model.error)throw new Error(model.error);
 const db=this.yy;
 db.setDirection(model.direction);
 const shapes={RECTANGLE:'square',ROUNDED:'round',STADIUM:'stadium',CIRCLE:'circle',DOUBLE_CIRCLE:'doublecircle',DIAMOND:'diamond',PARALLELOGRAM:'lean_right',PARALLELOGRAM_ALT:'lean_left',TRAPEZOID:'trapezoid',TRAPEZOID_ALT:'inv_trapezoid',SUBROUTINE:'subroutine',CYLINDER:'cylinder',HEXAGON:'hexagon',ASYMMETRIC:'odd',ELLIPSE:'ellipse'};
 for(const [id,styles] of Object.entries(model.definitions))db.addClass(id,styles);
 for(const n of model.nodes){
  if(n.createdByStyle)db.addVertex(n.id,undefined,undefined,n.styles);
  db.addVertex(n.id,{text:n.label,type:n.labelType},n.borders===null?shapes[n.shape]:'rect',n.createdByStyle?undefined:n.styles,n.classes,undefined,n.borders===null?{}:{borders:n.borders},Object.keys(n.metadata).length?Object.entries(n.metadata).map(([k,v])=>JSON.stringify(k)+': '+JSON.stringify(v)).join(', '):undefined);
 }
 if(model.accTitle!==null)db.setAccTitle(model.accTitle);
 if(model.accDescription!==null)db.setAccDescription(model.accDescription);
 for(const e of model.edges){const markers={NONE:'open',POINT:'point',CIRCLE:'circle',CROSS:'cross'};const type=(e.fromMarker!=='NONE'?'double_':'')+'arrow_'+markers[e.toMarker];
 db.addLink([e.from],[e.to],{type,stroke:{NORMAL:'normal',THICK:'thick',DOTTED:'dotted',INVISIBLE:'invisible'}[e.style],length:e.length,...(e.id===null?{}:{id:e.id+'@'}),...(e.label===null?{}:{text:{text:e.label,type:e.labelType}})});}
 if(model.defaultEdgeStyles.length)db.updateLink(['default'],model.defaultEdgeStyles);
 model.edges.forEach((e,index)=>{if(e.styles.length)db.updateLink([index],e.styles)});
 for(const i of model.interactions){
  if(i.callback){if(i.arguments===null)db.setClickEvent(i.nodeId,i.value);else db.setClickEvent(i.nodeId,i.value,i.arguments);}
  else {if(i.target===null)db.setLink(i.nodeId,i.value);else db.setLink(i.nodeId,i.value,i.target);}
  if(i.tooltip!==null)db.setTooltip(i.nodeId,i.tooltip);
 }
 if(model.defaultInterpolate!==null)db.updateLinkInterpolate(['default'],model.defaultInterpolate);
 model.edges.forEach((e,i)=>{if(e.interpolate!==null)db.updateLinkInterpolate([i],e.interpolate);if(e.id!==null&&(e.animate!==null||e.animation!==null))db.addVertex(e.id,undefined,undefined,undefined,undefined,undefined,{},JSON.stringify({...(e.animate===null?{}:{animate:e.animate}),...(e.animation===null?{}:{animation:e.animation})}).slice(1,-1));});
 for(const g of model.subgraphs)db.addSubGraph({text:g.id},g.nodes.concat(g.direction===null?[]:[{stmt:'dir',value:g.direction}]),{text:g.label,type:g.labelType});
 for(const g of model.subgraphs)if(g.collapsed)db.addVertex(g.id,undefined,undefined,undefined,undefined,undefined,{},'view: collapsed');
 return true;
}};
export default {parser,parse(source){return parser.parse(source)}};
""".replace('CACHE_PATH',json.dumps(str(cache))))

status = test('native')
summary = {'upstreamRevision':manifest['revision'],'nativeJarSha256':hashlib.sha256(snapshot.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
    report = json.loads((work / f'{mode}.json').read_text())
    summary[mode] = {k:report[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(work / 'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2))
raise SystemExit(status.returncode)
