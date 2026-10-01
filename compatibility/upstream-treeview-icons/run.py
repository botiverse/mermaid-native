#!/usr/bin/env python3
"""Replay unchanged upstream TreeView icon-selection assertions against the production Kotlin builder."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--stdlib', type=Path, required=True)
o = p.parse_args()
h = Path(__file__).resolve().parent
r, u = h.parents[1], o.upstream.resolve()
w = u / '.native-treeview-icons-audit'
w.mkdir(exist_ok=True)
m = json.loads((h / 'sources.json').read_text())
for group in ['files', 'sourceFiles']:
    for f, sha in m[group].items():
        assert hashlib.sha256((u / f).read_bytes()).hexdigest() == sha, f
jar = w / 'mermaid-core.jar'
shutil.copy2(r / 'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', jar)
cp = os.pathsep.join(map(str, [w, o.stdlib.resolve(), jar]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h / 'TreeViewIconsNativeBridge.java')], check=True)
calls = w / 'official-calls.jsonl'
calls.write_text('')
shared = """import {vi,expect} from 'vitest';
import * as utils from '../packages/mermaid/src/diagrams/treeView/icons.js';
"""
(w / 'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
for(const method of ['detectIcon','getNodeIcon']){
 const original=utils[method];
 vi.spyOn(utils,method).mockImplementation((input,config)=>{
  appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify([method,input,config??null])})+'\\n');
  return original(input,config);
 });
}
""".replace('CALLS', json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';
export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-treeview-icons-audit/SETUP.ts']}});
""".replace('FILES', json.dumps(list(m['files'])))
for mode, setup in [('official', 'capture'), ('native', 'adapter')]:
    (w / f'{mode}.config.ts').write_text(config.replace('SETUP', setup))
    (w / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-treeview-icons-audit/" + mode + ".config.ts']);")

def test(mode):
    with (w / f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm', 'exec', 'vitest', 'run', '--config', f'.native-treeview-icons-audit/{mode}.config.ts', '--workspace', f'.native-treeview-icons-audit/{mode}.workspace.ts', '--testNamePattern', '^(?!.*treeViewIcons pack).*', '--reporter=json', f'--outputFile={w / (mode + ".json")}'], cwd=u, stdout=log, stderr=subprocess.STDOUT, timeout=180)

if test('official').returncode:
    raise SystemExit('Official icon selection assertions failed')
assert json.loads((w / 'official.json').read_text())['numPassedTests'] == 20
sources = list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
lines = []
encode = lambda text: base64.b64encode(text.encode()).decode()
for source in sources:
    method, item, config = json.loads(source)
    config = config or {}
    assert set(config) <= {'showIcons', 'defaultIconPack', 'filenameIcons', 'extensionIcons'}
    filenames, extensions = config.get('filenameIcons', {}), config.get('extensionIcons', {})
    if method == 'detectIcon':
        name, directory, icon = item, False, None
    else:
        assert set(item) <= {'name', 'icon', 'nodeType'} and item['nodeType'] in ['file', 'directory']
        name, directory, icon = item['name'], item['nodeType'] == 'directory', item.get('icon')
    lines.append('\t'.join([method, encode(name), str(directory).lower(), '-' if icon is None else encode(icon), str(config.get('showIcons', False)).lower(), encode(config.get('defaultIconPack', '')), str(len(filenames)), str(len(extensions))]))
    for mapping in [filenames, extensions]:
        for key, value in mapping.items():
            lines.append(encode(key) + '\t' + encode(value))
result = subprocess.run(['java', '-cp', cp, 'TreeViewIconsNativeBridge'], input='\n'.join(lines) + '\n', capture_output=True, text=True, check=True, timeout=60)
values = [json.loads(line) for line in result.stdout.splitlines()]
assert len(values) == len(sources)
cache = w / 'native-results.json'
cache.write_text(json.dumps(list(zip(sources, values))))
(w / 'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
for(const method of ['detectIcon','getNodeIcon'])vi.spyOn(utils,method).mockImplementation((input,config)=>{
 const key=JSON.stringify([method,input,config??null]);if(!results.has(key))throw new Error('Missing Native icon result');
 return results.get(key)??undefined;
});
""".replace('CACHE', json.dumps(str(cache))))
status = test('native')
summary = {'upstreamRevision': m['revision'], 'runtimeSha256': {'mermaid-core.jar': hashlib.sha256(jar.read_bytes()).hexdigest()}, 'uniqueInputs': len(sources), 'selection': '20 original icon selection assertions, unchanged; three SVG icon-pack assertions unselected'}
for mode in ['official', 'native']:
    d = json.loads((w / f'{mode}.json').read_text())
    summary[mode] = {k: d[k] for k in ['numTotalTests', 'numPassedTests', 'numFailedTests', 'numPendingTests', 'success']}
(w / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
raise SystemExit(status.returncode)
