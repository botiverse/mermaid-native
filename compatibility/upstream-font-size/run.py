#!/usr/bin/env python3
"""Run six unchanged upstream font-size assertions against the production Kotlin parser."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path
p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
p.add_argument('--stdlib', type=Path, required=True)
o = p.parse_args()
u, r, h = o.upstream.resolve(), o.repo.resolve(), Path(__file__).resolve().parent
w = u / '.native-font-size-audit'; w.mkdir(exist_ok=True)
m = json.loads((h/'sources.json').read_text())
for group in ['files', 'sourceFiles']:
    for f, sha in m[group].items():
        assert hashlib.sha256((u/f).read_bytes()).hexdigest() == sha, f
jars = []
for module in ['mermaid-core']:
    dest = w/(module+'.jar')
    shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', dest)
    jars.append(dest)
cp = os.pathsep.join(map(str, [w, o.stdlib.resolve(), *jars]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h/'FontSizeNativeBridge.java')], check=True)
calls = w/'official-calls.jsonl'; calls.write_text('')
shared = """import {vi,expect} from 'vitest';
import utils from '../packages/mermaid/src/utils.js';
function input(value){return {kind:typeof value,value};}
"""
(w/'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
const original=utils.parseFontSize;
vi.spyOn(utils,'parseFontSize').mockImplementation((value)=>{
 appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify(input(value))})+'\\n');
 return original(value);
});
""".replace('CALLS',json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';
import jison from '../.vite/jisonPlugin.js';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
export default defineConfig({plugins:[jison(),jsonSchemaPlugin()],define:{'injected.includeLargeFeatures':'true','injected.profiling':'false','import.meta.vitest':'undefined',packageVersion:JSON.stringify('0.0.0')},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-font-size-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
    (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
    (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-font-size-audit/"+mode+".config.ts']);")
def test(mode):
    with (w/f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-font-size-audit/{mode}.config.ts','--workspace',f'.native-font-size-audit/{mode}.workspace.ts','--testNamePattern','^when parsing font sizes','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode: raise SystemExit('Original font-size assertions failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==6
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
lines=[]
for source in sources:
    value=json.loads(source)
    raw=value.get('value','')
    if not isinstance(raw,str): raw=json.dumps(raw)
    lines.append(value['kind']+'\t'+base64.b64encode(raw.encode()).decode())
result=subprocess.run(['java','-cp',cp,'FontSizeNativeBridge'],input='\n'.join(lines)+'\n',text=True,capture_output=True,check=True,timeout=60)
results=[]
for line in result.stdout.splitlines():
    if line=='null': results.append([None,None])
    else:
        number,css=line.split('\t')
        results.append([float(number),base64.b64decode(css).decode()])
assert len(results)==len(sources)
cache=w/'native-results.json';cache.write_text(json.dumps(list(zip(sources,results))))
(w/'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
vi.spyOn(utils,'parseFontSize').mockImplementation((value)=>{
 const key=JSON.stringify(input(value));if(!results.has(key))throw new Error('Missing Native parser result');
 return results.get(key).map(value=>value===null?undefined:value);
});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
summary={'upstreamRevision':m['revision'],'runtimeSha256':{j.name:hashlib.sha256(j.read_bytes()).hexdigest() for j in jars},'uniqueInputs':len(sources),'selection':'Six original when parsing font sizes tests; other utils tests excluded'}
for mode in ['official','native']:
    d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
