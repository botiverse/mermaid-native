#!/usr/bin/env python3
"""Run unmodified upstream Gantt assertions with the official and Kotlin parsers."""
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
work = upstream / '.native-gantt-audit'
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
run(['javac', '-cp', cp, '-d', str(work), str(here / 'GanttNativeBridge.java')])
fixtures = ['packages/mermaid/src/diagrams/gantt/parser/gantt.spec.js']
captured = work / 'official-calls.jsonl'
captured.write_text('')
(work / 'capture.ts').write_text('''
import {appendFileSync} from 'node:fs';
import {beforeAll,afterAll,expect} from 'vitest';
import {parser as actualParser} from '../packages/mermaid/src/diagrams/gantt/parser/gantt.jison';
let original;
beforeAll(()=>{original=actualParser.parse;actualParser.parse=function(source){
 appendFileSync(CAPTURE_PATH,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');
 return original.call(this,source);
};});
afterAll(()=>{actualParser.parse=original;});
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
(work / 'official.config.ts').write_text(config.replace('PARSER_PLUGIN','jison()').replace('SETUP',"setupFiles:['.native-gantt-audit/capture.ts']"))
(work / 'native.config.ts').write_text(config.replace('PARSER_PLUGIN',"{name:'native-gantt',enforce:'pre',resolveId(id){if(id.endsWith('gantt.jison'))return resolve('.native-gantt-audit/adapter.ts');}}").replace('SETUP',''))
for mode in ['official','native']:
    (work / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-gantt-audit/"+mode+".config.ts']);\n")

def test(mode):
    return run(['pnpm','exec','vitest','run','--config',f'.native-gantt-audit/{mode}.config.ts','--workspace',f'.native-gantt-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={work / (mode+".json")}'],log=work / f'{mode}.log',check=False)

if test('official').returncode:
    raise SystemExit(f'Official reference suite failed; inspect {work / "official.log"}')
sources = list(dict.fromkeys(json.loads(line)['source'] for line in captured.read_text().splitlines()))
result = subprocess.run(['java','-cp',cp,'GanttNativeBridge','--batch'],input=''.join(base64.b64encode(s.encode()).decode()+'\n' for s in sources),text=True,capture_output=True,check=True)
models = [json.loads(line) for line in result.stdout.splitlines()]
assert len(models) == len(sources)
cache = work / 'native-models.json'
cache.write_text(json.dumps(list(zip(sources,models))))
(work / 'adapter.ts').write_text("""
import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE_PATH,'utf8')));
const taskStores=new WeakMap();
export const parser={yy:null,parse(source){
 const model=models.get(source);if(!model)throw new Error('Missing Native result');if(model.error)throw new Error(model.error);
 const db=this.yy;
 if(!taskStores.has(db)){taskStores.set(db,[]);const clear=db.clear;db.clear=function(...args){taskStores.set(db,[]);return clear.apply(this,args);};db.getTasks=()=>taskStores.get(db);}
 db.setDateFormat(model.dateFormat);if(model.title!==null)db.setDiagramTitle(model.title);
 if(model.accTitle!==null)db.setAccTitle(model.accTitle);if(model.accDescription!==null)db.setAccDescription(model.accDescription);
 if(model.excludes.length)db.setExcludes(model.excludes.join(' '));if(model.includes.length)db.setIncludes(model.includes.join(' '));
 if(model.inclusive)db.enableInclusiveEndDates();if(model.today!==null)db.setTodayMarker(model.today);if(model.axis!==null)db.setAxisFormat(model.axis);if(model.tick!==null)db.setTickInterval(model.tick);
 db.setWeekday(model.weekday);db.setWeekend(model.weekend);
 const date=value=>{const [y,m,d]=value.split('-').map(Number);return new Date(y,m-1,d);};
 for(const section of model.sections){db.addSection(section.name);for(const t of section.tasks)taskStores.get(db).push({id:t.id,task:t.name,type:section.name,startTime:date(t.start),endTime:date(t.end),milestone:t.milestone,done:t.statuses.includes('DONE'),active:t.statuses.includes('ACTIVE'),crit:t.statuses.includes('CRITICAL')});}
 for(const a of model.actions){if(a.callback)db.setClickEvent(a.id,a.value,a.args);else db.setLink(a.id,a.value);}
 const doc=true;
 return doc;
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
