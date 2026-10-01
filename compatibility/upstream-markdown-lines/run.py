#!/usr/bin/env python3
"""Run nine unchanged upstream Markdown line assertions against production Kotlin."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path
p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
p.add_argument('--stdlib', type=Path, required=True)
o = p.parse_args()
u, r, h = o.upstream.resolve(), o.repo.resolve(), Path(__file__).resolve().parent
w = u / '.native-markdown-lines-audit'; w.mkdir(exist_ok=True)
m = json.loads((h/'sources.json').read_text())
for group in ['files', 'sourceFiles']:
    for f, sha in m[group].items():
        assert hashlib.sha256((u/f).read_bytes()).hexdigest() == sha, f
jars = []
for module in ['mermaid-core', 'mermaid-layout-api', 'mermaid-layout-simple']:
    dest = w/(module+'.jar')
    shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', dest)
    jars.append(dest)
cp = os.pathsep.join(map(str, [w, o.stdlib.resolve(), *jars]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h/'MarkdownLinesNativeBridge.java')], check=True)
calls = w/'official-calls.jsonl'; calls.write_text('')
shared = """import {vi,expect} from 'vitest';
import * as markdown from '../packages/mermaid/src/rendering-util/handle-markdown-text.js';
"""
(w/'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
const original=markdown.markdownToLines;
vi.spyOn(markdown,'markdownToLines').mockImplementation((source,...args)=>{
 appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source})+'\\n');
 return original(source,...args);
});
""".replace('CALLS',json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';
import jison from '../.vite/jisonPlugin.js';
import jsonSchemaPlugin from '../.vite/jsonSchemaPlugin.js';
export default defineConfig({plugins:[jison(),jsonSchemaPlugin()],define:{'injected.includeLargeFeatures':'true','injected.profiling':'false','import.meta.vitest':'undefined',packageVersion:JSON.stringify('0.0.0')},test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-markdown-lines-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
    (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
    (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-markdown-lines-audit/"+mode+".config.ts']);")
def test(mode):
    with (w/f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-markdown-lines-audit/{mode}.config.ts','--workspace',f'.native-markdown-lines-audit/{mode}.workspace.ts','--testNamePattern','^markdownToLines - (?!No auto wrapping)','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode: raise SystemExit('Original markdown-lines assertions failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==9
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
lines=[base64.b64encode(source.encode()).decode() for source in sources]
result=subprocess.run(['java','-cp',cp,'MarkdownLinesNativeBridge'],input='\n'.join(lines)+'\n',text=True,capture_output=True,check=True,timeout=60)
results=[json.loads(line) for line in result.stdout.splitlines()]
assert len(results)==len(sources)
cache=w/'native-results.json';cache.write_text(json.dumps(list(zip(sources,results))))
(w/'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
vi.spyOn(markdown,'markdownToLines').mockImplementation((source,config)=>{
 if(config!==undefined)throw new Error('Config is outside this selected Native contract');
 if(!results.has(source))throw new Error('Missing Native Markdown result');
 return results.get(source);
});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
summary={'upstreamRevision':m['revision'],'runtimeSha256':{j.name:hashlib.sha256(j.read_bytes()).hexdigest() for j in jars},'uniqueInputs':len(sources),'selection':'Nine original markdownToLines tests; HTML, non-Markdown and disabled auto-wrap cases excluded'}
for mode in ['official','native']:
    d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
