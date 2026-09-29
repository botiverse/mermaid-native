#!/usr/bin/env python3
"""Replay unchanged Railroad title and accessibility model assertions through MermaidParser."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
p.add_argument('--stdlib', type=Path, required=True)
p.add_argument('--runtime-jar', type=Path, help='Already validated production core JAR; no Gradle is launched')
o = p.parse_args()
u, r, h = o.upstream.resolve(), o.repo.resolve(), Path(__file__).resolve().parent
w = u / '.native-railroad-model-audit'
w.mkdir(exist_ok=True)
m = json.loads((h / 'sources.json').read_text())
for f, sha in m['files'].items():
    assert hashlib.sha256((u / f).read_bytes()).hexdigest() == sha, f
jar = w / 'native-runtime.jar'
shutil.copy2(o.runtime_jar or r / 'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', jar)
cp = os.pathsep.join(map(str, [w, jar, o.stdlib.resolve()]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h.parent / 'upstream-railroad/RailroadNativeBridge.java')], check=True)
calls = w / 'official-calls.jsonl'
calls.write_text('')
selected = [
    'should handle title',
    'should handle accessibility title',
    'should handle accessibility description',
    'should share state between setTitle and setDiagramTitle',
    'should return title via getDiagramTitle',
]
shared = """import {vi,expect} from 'vitest';
import db from '../packages/mermaid/src/diagrams/railroad/railroadDb.js';
let fields={};
function source(){return ['railroad-beta',...Object.entries(fields).map(([key,value])=>key+(key==='title'?' ':': ')+value)].join('\\n');}
const methods=['setTitle','setDiagramTitle','setAccTitle','setAccDescription'];
function update(method,args){fields[({setTitle:'title',setDiagramTitle:'title',setAccTitle:'accTitle',setAccDescription:'accDescr'})[method]]=args[0];}
"""
(w / 'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
function record(){appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:source()})+'\\n');}
const clear=db.clear;
vi.spyOn(db,'clear').mockImplementation(()=>{fields={};record();return clear();});
for(const method of methods){const original=db[method];vi.spyOn(db,method).mockImplementation((...args)=>{update(method,args);record();return original(...args);});}
""".replace('CALLS', json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-railroad-model-audit/SETUP.ts']}});
""".replace('FILES', json.dumps(list(m['files'])))
for mode, setup in [('official', 'capture'), ('native', 'adapter')]:
    (w / f'{mode}.config.ts').write_text(config.replace('SETUP', setup))
    (w / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-railroad-model-audit/" + mode + ".config.ts']);")

def test(mode):
    with (w / f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm', 'exec', 'vitest', 'run', '--config', f'.native-railroad-model-audit/{mode}.config.ts', '--workspace', f'.native-railroad-model-audit/{mode}.workspace.ts', '--testNamePattern', '|'.join(' '+name+'$' for name in selected), '--reporter=json', f'--outputFile={w / (mode + ".json")}'], cwd=u, stdout=log, stderr=subprocess.STDOUT, timeout=240)

if test('official').returncode:
    raise SystemExit('Original Railroad model suite failed')
assert json.loads((w / 'official.json').read_text())['numPassedTests'] == len(selected)
sources = list(dict.fromkeys(json.loads(line)['source'] for line in calls.read_text().splitlines()))
result = subprocess.run(['java', '-cp', cp, 'RailroadNativeBridge'], input=''.join(base64.b64encode(s.encode()).decode() + '\n' for s in sources), text=True, capture_output=True, check=True, timeout=60)
models = [json.loads(line) for line in result.stdout.splitlines()]
assert len(models) == len(sources)
cache = w / 'native-models.json'
cache.write_text(json.dumps(list(zip(sources, models))))
(w / 'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const models=new Map(JSON.parse(readFileSync(CACHE,'utf8')));let current;
function refresh(){if(!models.has(source()))throw new Error('Missing Native result');current=structuredClone(models.get(source()));if(current.error)throw new Error(current.error);}
vi.spyOn(db,'clear').mockImplementation(()=>{fields={};refresh();});
for(const method of methods)vi.spyOn(db,method).mockImplementation((...args)=>{update(method,args);refresh();});
for(const [method,field] of Object.entries({getTitle:'title',getDiagramTitle:'title',getAccTitle:'accTitle',getAccDescription:'accDescr'}))vi.spyOn(db,method).mockImplementation(()=>current[field]??'');
""".replace('CACHE', json.dumps(str(cache))))
status = test('native')
summary = {'upstreamRevision': m['revision'], 'nativeJarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'uniqueInputs': len(sources)}
for mode in ['official', 'native']:
    d = json.loads((w / f'{mode}.json').read_text())
    summary[mode] = {k: d[k] for k in ['numTotalTests', 'numPassedTests', 'numFailedTests', 'numPendingTests', 'success']}
(w / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
raise SystemExit(status.returncode)
