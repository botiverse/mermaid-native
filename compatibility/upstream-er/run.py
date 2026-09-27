#!/usr/bin/env python3
"""Run unmodified upstream ER assertions with the official and Kotlin parsers."""
import argparse, base64, hashlib, json, os, subprocess
from pathlib import Path

args = argparse.ArgumentParser(description=__doc__)
args.add_argument('--upstream', required=True, type=Path)
args.add_argument('--stdlib', required=True, type=Path, help='Kotlin 2.1.21 stdlib JAR')
opt = args.parse_args()
here = Path(__file__).resolve().parent
repo = here.parents[1]
upstream = opt.upstream.resolve()
work = upstream / '.native-er-audit'
work.mkdir(exist_ok=True)
manifest = json.loads((here / 'sources.json').read_text())
for file, expected in manifest['files'].items():
    actual = hashlib.sha256((upstream / file).read_bytes()).hexdigest()
    if actual != expected:
        raise SystemExit(f'Upstream source differs from pinned revision: {file}')

def run(argv, cwd=upstream, log=None, check=True):
    with open(log or os.devnull, 'w') as output:
        return subprocess.run(argv, cwd=cwd, stdout=output, stderr=subprocess.STDOUT, check=check)

run([str(repo / 'gradlew'), ':mermaid-core:bundleLibRuntimeToJarDebug'], repo, work / 'native-build.log')
jar = repo / 'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar'
cp = os.pathsep.join(map(str, [work, jar, opt.stdlib.resolve()]))
run(['javac', '-cp', cp, '-d', str(work), str(here / 'ErNativeBridge.java')])
fixture = 'packages/mermaid/src/diagrams/er/parser/erDiagram.spec.js'
captured = work / 'official-calls.jsonl'
captured.write_text('')
(work / 'capture.ts').write_text('''
import {appendFileSync} from 'node:fs';
import {beforeAll,afterAll,expect} from 'vitest';
import erDiagram from '../packages/mermaid/src/diagrams/er/parser/erDiagram.jison';
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
 test:{environment:'jsdom',globals:true,maxWorkers:1,include:[FIXTURE],SETUP},
 define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"}
});
'''.replace('FIXTURE', json.dumps(fixture))
(work / 'official.config.ts').write_text(config.replace('PARSER_PLUGIN','jison()').replace('SETUP',"setupFiles:['.native-er-audit/capture.ts']"))
(work / 'native.config.ts').write_text(config.replace('PARSER_PLUGIN',"{name:'native-er',enforce:'pre',resolveId(id){if(id.endsWith('erDiagram.jison'))return resolve('.native-er-audit/adapter.ts');}}").replace('SETUP',''))
for mode in ['official','native']:
    (work / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-er-audit/"+mode+".config.ts']);\n")

def test(mode):
    return run(['pnpm','exec','vitest','run','--config',f'.native-er-audit/{mode}.config.ts','--workspace',f'.native-er-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={work / (mode+".json")}'],log=work / f'{mode}.log',check=False)

if test('official').returncode:
    raise SystemExit(f'Official reference suite failed; inspect {work / "official.log"}')
sources = list(dict.fromkeys(json.loads(line)['source'] for line in captured.read_text().splitlines()))
result = subprocess.run(['java','-cp',cp,'ErNativeBridge','--batch'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True)
models = [json.loads(line) for line in result.stdout.splitlines()]
assert len(models) == len(sources)
cache = work / 'native-models.json'
cache.write_text(json.dumps(list(zip(sources,models))))
(work / 'adapter.ts').write_text('''
import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE_PATH,'utf8')));
const parser={yy:null,parse(source){
 const model=models.get(source);
 if(!model)throw new Error('Native result missing; regenerate the capture');
 if(model.error)throw new Error(model.error);
 if(model.accessibilityTitle!==null)this.yy.setAccTitle(model.accessibilityTitle);
 if(model.accessibilityDescription!==null)this.yy.setAccDescription(model.accessibilityDescription);
 for(const [id,styles] of model.classDefinitions)this.yy.addClass([id],styles);
 for(const e of model.entities){this.yy.addEntity(e.id,e.alias);this.yy.addAttributes(e.id,[...e.attributes].reverse());this.yy.addCssStyles([e.id],e.styles);this.yy.setClass([e.id],e.classes);}
 for(const r of model.relationships)this.yy.addRelationship(r.from,r.label,r.to,{cardA:r.cardA,cardB:r.cardB,relType:r.relType});
 return true;
}};
export default {parser};
'''.replace('CACHE_PATH',json.dumps(str(cache))))
status = test('native')
summary = {'upstreamRevision':manifest['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(sources)}
for mode in ['official','native']:
    report = json.loads((work / f'{mode}.json').read_text())
    summary[mode] = {k:report[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(work / 'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2))
raise SystemExit(status.returncode)
