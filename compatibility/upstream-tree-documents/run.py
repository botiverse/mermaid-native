#!/usr/bin/env python3
"""Run two complete, unchanged upstream files against direct Kotlin document operations."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--stdlib', type=Path, required=True)
p.add_argument('--verify-mutations', action='store_true')
o = p.parse_args()
h = Path(__file__).resolve().parent
r = h.parents[1]
u = o.upstream.resolve()
w = u / '.native-tree-documents-audit'
w.mkdir(exist_ok=True)
manifest = json.loads((h / 'sources.json').read_text())
for f, sha in manifest['files'].items():
    for path in (u / f, r / 'compatibility/upstream-batch/originals' / f):
        assert hashlib.sha256(path.read_bytes()).hexdigest() == sha, path
calls = w / 'official-calls.jsonl'
calls.write_text('')
shared = """import {vi,expect,afterEach} from 'vitest';
import {MindmapDB} from '../packages/mermaid/src/diagrams/mindmap/mindmapDb.js';
import tree from '../packages/mermaid/src/diagrams/treeView/db.js';
const name=()=>expect.getState().currentTestName??'';
const normalize=args=>args.map(x=>x==null?null:String(x));
const methods=[['clear','T.clear',0],['addNode','T.add',6],['getRoot','T.root',0],['getCount','T.count',0],['getConfig','T.config',0],['setDiagramTitle','T.title.set',1],['getDiagramTitle','T.title.get',0],['setAccTitle','T.accTitle.set',1],['getAccTitle','T.accTitle.get',0],['setAccDescription','T.accDescr.set',1],['getAccDescription','T.accDescr.get',0]];
const proxyNode=(node,set)=>node==null?node:new Proxy(node,{
 get(target,key){const value=Reflect.get(target,key);return key==='children'?value.map(child=>proxyNode(child,set)):value;},
 set(target,key,value){set(target.id,String(key),value);return Reflect.set(target,key,value);}
});
"""
(w / 'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
const save=(op,args=[])=>appendFileSync(CALLS,JSON.stringify({test:name(),op,args:normalize(args)})+'\\n');
let internal=0;
for(const [method,op] of [['clear','M.clear'],['addNode','M.add'],['getMindmap','M.root'],['getData','M.data']]){
 const original=MindmapDB.prototype[method];
 vi.spyOn(MindmapDB.prototype,method).mockImplementation(function(...args){
  if(internal)return original.apply(this,args);
  save(op,args);internal++;let value;try{value=original.apply(this,args);}finally{internal--;}
  return method==='getMindmap'?proxyNode(value,(id,key,v)=>save('M.set',[id,key,v])):value;
 });
}
for(const [method,op,arity] of methods){const original=tree[method];vi.spyOn(tree,method).mockImplementation((...args)=>{save(op,Array.from({length:arity},(_,i)=>args[i]));return original(...args);});}
""".replace('CALLS', json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';import {resolve} from 'node:path';
export default defineConfig({plugins:[jsonSchemaPlugin()],resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-tree-documents-audit/SETUP.ts']}});
""".replace('FILES', json.dumps(list(manifest['files'])))
for mode, setup in [('official', 'capture'), ('native', 'adapter')]:
    (w / f'{mode}.config.ts').write_text(config.replace('SETUP', setup))
    (w / f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-tree-documents-audit/" + mode + ".config.ts']);")
def test(mode):
    with (w / f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm', 'exec', 'vitest', 'run', '--config', f'.native-tree-documents-audit/{mode}.config.ts', '--workspace', f'.native-tree-documents-audit/{mode}.workspace.ts', '--reporter=json', f'--outputFile={w / (mode + ".json")}'], cwd=u, stdout=log, stderr=subprocess.STDOUT, timeout=240)
assert test('official').returncode == 0, 'Reference failed: see official.log'
assert json.loads((w / 'official.json').read_text())['numPassedTests'] == 29
jar = w / 'native-runtime.jar'
shutil.copy2(r / 'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', jar)
cp = os.pathsep.join(map(str, [w, jar, o.stdlib.resolve()]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h / 'TreeDocumentsBridge.java')], check=True)
trace = [json.loads(x) for x in calls.read_text().splitlines()]
commands = ['\t'.join([row['op']] + ['-' if x is None else base64.b64encode(x.encode()).decode() for x in row['args']]) for row in trace]
result = subprocess.run(['java', '-cp', cp, 'TreeDocumentsBridge'], input='\n'.join(commands) + '\n', text=True, capture_output=True, check=True, timeout=60)
snapshots = [json.loads(x) for x in result.stdout.splitlines()]
assert len(snapshots) == len(trace)
cache = w / 'native-snapshots.json'
cache.write_text(json.dumps([{**call, 'snapshot': snap} for call, snap in zip(trace, snapshots)]))
(w / 'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const trace=JSON.parse(readFileSync(CACHE,'utf8'));const byTest=new Map();
for(const row of trace){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
const step=(op,args=[])=>{const next=byTest.get(name())?.shift();if(!next||next.op!==op||JSON.stringify(next.args)!==JSON.stringify(normalize(args)))throw new Error('Native operation trace mismatch: '+name()+' '+op);if(next.snapshot.error)throw new Error(next.snapshot.error);return next.snapshot.value;};
afterEach(()=>{if(byTest.get(name())?.length)throw new Error('Unconsumed Native operations: '+name());});
for(const [method,op] of [['clear','M.clear'],['addNode','M.add'],['getMindmap','M.root'],['getData','M.data']]){
 vi.spyOn(MindmapDB.prototype,method).mockImplementation(function(...args){const value=step(op,args);return method==='getMindmap'?proxyNode(value,(id,key,v)=>step('M.set',[id,key,v])):value;});
}
for(const [method,op,arity] of methods)vi.spyOn(tree,method).mockImplementation((...args)=>step(op,Array.from({length:arity},(_,i)=>args[i])));
""".replace('CACHE', json.dumps(str(cache))))
status = test('native')
def failed():
    return {a['fullName'] for s in json.loads((w / 'native.json').read_text())['testResults'] for a in s['assertionResults'] if a['status'] == 'failed'}
baseline = failed()
summary = {'upstreamRevision': manifest['revision'], 'nativeSource': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=r, text=True).strip(), 'nativeJarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'nativeOperations': len(trace), 'operations': {op: sum(x['op'] == op for x in trace) for op in sorted({x['op'] for x in trace})}}
for mode in ['official', 'native']:
    d = json.loads((w / f'{mode}.json').read_text())
    summary[mode] = {k: d[k] for k in ['numTotalTests', 'numPassedTests', 'numFailedTests', 'numPendingTests', 'success']}
if o.verify_mutations:
    original = cache.read_text()
    controls = {}
    try:
        for key in ['counter', 'identity', 'width', 'section', 'metadata']:
            mutated = json.loads(original)
            targets = set()
            for row in mutated:
                value = row['snapshot'].get('value')
                if key == 'counter' and row['op'] == 'T.count' and 'should start at 1' in row['test']:
                    row['snapshot']['value'] = 0
                elif key == 'identity' and row['op'] == 'T.root' and 'unique incrementing ids' in row['test']:
                    value['children'][1]['id'] = 1
                elif key == 'width' and row['op'] == 'M.data' and 'preserve node properties' in row['test']:
                    value['nodes'][0]['width'] = 200
                elif key == 'section' and row['op'] == 'M.data' and 'based on sibling position' in row['test']:
                    value['nodes'][2]['section'] = 99
                elif key == 'metadata' and row['op'] == 'T.accTitle.get':
                    row['snapshot']['value'] = ''
                else:
                    continue
                targets.add(row['test'].replace(' > ', ' '))
            assert len(targets) == 1 and not targets & baseline, (key, targets, baseline)
            cache.write_text(json.dumps(mutated))
            test('native')
            assert failed() - baseline == targets, (key, failed(), targets)
            controls[key] = sorted(targets)
            shutil.copy2(w / 'native.json', w / f'mutation-{key}.json')
    finally:
        cache.write_text(original)
        status = test('native')
        assert failed() == baseline
    summary['mutationControls'] = controls
(w / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
(w / 'failures.json').write_text(json.dumps(sorted(baseline), indent=2) + '\n')
print(json.dumps(summary, indent=2))
raise SystemExit(status.returncode)
