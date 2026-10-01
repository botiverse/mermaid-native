#!/usr/bin/env python3
"""Replay unchanged upstream hierarchy assertions against the production Kotlin builder."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--stdlib', type=Path, required=True)
o = p.parse_args()
h = Path(__file__).resolve().parent
r, u = h.parents[1], o.upstream.resolve()
w = u / '.native-treemap-hierarchy-audit'
w.mkdir(exist_ok=True)
m = json.loads((h / 'sources.json').read_text())
for group in ['files', 'sourceFiles']:
    for f, sha in m[group].items():
        assert hashlib.sha256((u / f).read_bytes()).hexdigest() == sha, f
jar = w / 'mermaid-core.jar'
shutil.copy2(r / 'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', jar)
cp = os.pathsep.join(map(str, [w, o.stdlib.resolve(), jar]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h / 'TreemapHierarchyNativeBridge.java')], check=True)
calls = w / 'official-calls.jsonl'
calls.write_text('')
shared = """import {vi,expect} from 'vitest';
import * as utils from '../packages/mermaid/src/diagrams/treemap/utils.js';
"""
(w / 'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
const original=utils.buildHierarchy;
vi.spyOn(utils,'buildHierarchy').mockImplementation((items)=>{
 appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify(items)})+'\\n');
 return original(items);
});
""".replace('CALLS', json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';
export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-treemap-hierarchy-audit/SETUP.ts']}});
""".replace('FILES', json.dumps(list(m['files'])))
for mode, setup in [('official', 'capture'), ('native', 'adapter')]:
    (w / f'{mode}.config.ts').write_text(config.replace('SETUP', setup))
    (w / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-treemap-hierarchy-audit/" + mode + ".config.ts']);")

def test(mode):
    with (w / f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm', 'exec', 'vitest', 'run', '--config', f'.native-treemap-hierarchy-audit/{mode}.config.ts', '--workspace', f'.native-treemap-hierarchy-audit/{mode}.workspace.ts', '--reporter=json', f'--outputFile={w / (mode + ".json")}'], cwd=u, stdout=log, stderr=subprocess.STDOUT, timeout=180)

if test('official').returncode:
    raise SystemExit('Official hierarchy assertions failed')
assert json.loads((w / 'official.json').read_text())['numPassedTests'] == 4
sources = list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
lines = []
encode = lambda text: base64.b64encode(text.encode()).decode()
for source in sources:
    items = json.loads(source)
    lines.append(str(len(items)))
    for item in items:
        # Reject unsupported transports instead of silently claiming their coverage.
        assert set(item) <= {'level', 'name', 'type', 'value', 'classSelector'}
        assert item['type'] in ['Section', 'Leaf']
        assert (item['type'] == 'Leaf') == ('value' in item)
        lines.append('\t'.join([str(item['level']), encode(item['name']), str(item['value']) if 'value' in item else '', encode(item['classSelector']) if 'classSelector' in item else '']))
result = subprocess.run(['java', '-cp', cp, 'TreemapHierarchyNativeBridge'], input='\n'.join(lines) + '\n', capture_output=True, text=True, check=True, timeout=60)
values = [json.loads(line) for line in result.stdout.splitlines()]
assert len(values) == len(sources)
cache = w / 'native-results.json'
cache.write_text(json.dumps(list(zip(sources, values))))
(w / 'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
vi.spyOn(utils,'buildHierarchy').mockImplementation((items)=>{
 const key=JSON.stringify(items);if(!results.has(key))throw new Error('Missing Native hierarchy result');
 return structuredClone(results.get(key));
});
""".replace('CACHE', json.dumps(str(cache))))
status = test('native')
summary = {'upstreamRevision': m['revision'], 'runtimeSha256': {'mermaid-core.jar': hashlib.sha256(jar.read_bytes()).hexdigest()}, 'uniqueInputs': len(sources), 'selection': 'All four original hierarchy tests, unchanged; no assertions excluded'}
for mode in ['official', 'native']:
    d = json.loads((w / f'{mode}.json').read_text())
    summary[mode] = {k: d[k] for k in ['numTotalTests', 'numPassedTests', 'numFailedTests', 'numPendingTests', 'success']}
(w / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
raise SystemExit(status.returncode)
