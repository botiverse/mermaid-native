#!/usr/bin/env python3
"""Replay original accessibility assertions on DOM imported from real Native SVG output."""
import argparse, base64, hashlib, json, os, shutil, subprocess
from pathlib import Path
p = argparse.ArgumentParser()
p.add_argument('--upstream', type=Path, required=True)
p.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
p.add_argument('--runtime-repo', type=Path, help='Optional already-built baseline checkout')
p.add_argument('--stdlib', type=Path, required=True)
o = p.parse_args()
u, r, h = o.upstream.resolve(), (o.runtime_repo or o.repo).resolve(), Path(__file__).resolve().parent
w = u / '.native-svg-accessibility-audit'; w.mkdir(exist_ok=True)
m = json.loads((h/'sources.json').read_text())
for f, sha in m['files'].items(): assert hashlib.sha256((u/f).read_bytes()).hexdigest() == sha, f
jars = []
for module in ['mermaid-core', 'mermaid-layout-api', 'mermaid-render-svg']:
    dest = w/(module+'.jar')
    shutil.copy2(r/module/'build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar', dest)
    jars.append(dest)
cp = os.pathsep.join(map(str, [w, o.stdlib.resolve(), *jars]))
subprocess.run(['javac', '-cp', cp, '-d', str(w), str(h/'SvgAccessibilityNativeBridge.java')], check=True)
calls = w/'official-calls.jsonl'; calls.write_text('')
shared = """import {vi,expect} from 'vitest';
import * as accessibility from '../packages/mermaid/src/accessibility.js';
function input(method,args){return method==='setA11yDiagramInfo'?['','','mermaid',args[1]]:[args[1]??'',args[2]??'',args[3],''];}
"""
(w/'capture.ts').write_text(shared + """import {appendFileSync} from 'node:fs';
for(const method of ['setA11yDiagramInfo','addSVGa11yTitleDescription']){
 const original=accessibility[method];
 vi.spyOn(accessibility,method).mockImplementation((...args)=>{
  appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,source:JSON.stringify(input(method,args))})+'\\n');
  return original(...args);
 });
}
""".replace('CALLS',json.dumps(str(calls))))
config = """import {defineConfig} from 'vitest/config';
export default defineConfig({test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-svg-accessibility-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
    (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup))
    (w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-svg-accessibility-audit/"+mode+".config.ts']);")
def test(mode):
    with (w/f'{mode}.log').open('w') as log:
        return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-svg-accessibility-audit/{mode}.config.ts','--workspace',f'.native-svg-accessibility-audit/{mode}.workspace.ts','--testNamePattern','^(?!.*should do nothing if there is no insert defined)','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode: raise SystemExit('Original SVG accessibility suite failed')
assert json.loads((w/'official.json').read_text())['numPassedTests']==19
sources=list(dict.fromkeys(json.loads(x)['source'] for x in calls.read_text().splitlines()))
payload=''.join('\t'.join(base64.b64encode(v.encode()).decode() for v in json.loads(s))+'\n' for s in sources)
result=subprocess.run(['java','-cp',cp,'SvgAccessibilityNativeBridge'],input=payload,text=True,capture_output=True,check=True,timeout=60)
svgs=[base64.b64decode(line).decode() for line in result.stdout.splitlines()]; assert len(svgs)==len(sources)
cache=w/'native-svgs.json';cache.write_text(json.dumps(list(zip(sources,svgs))))
(w/'adapter.ts').write_text(shared + """import {readFileSync} from 'node:fs';
const svgs=new Map(JSON.parse(readFileSync(CACHE,'utf8')));
for(const method of ['setA11yDiagramInfo','addSVGa11yTitleDescription'])vi.spyOn(accessibility,method).mockImplementation((...args)=>{
 const key=JSON.stringify(input(method,args));if(!svgs.has(key))throw new Error('Missing Native SVG output');
 const parsed=new window.DOMParser().parseFromString(svgs.get(key),'image/svg+xml');
 if(parsed.querySelector('parsererror'))throw new Error('Native SVG is invalid XML');
 args[0].node().replaceWith(document.importNode(parsed.documentElement,true));
});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
summary={'upstreamRevision':m['revision'],'runtimeSha256':{j.name:hashlib.sha256(j.read_bytes()).hexdigest() for j in jars},'uniqueInputs':len(sources),'unselected':'D3 object without insert; no Native equivalent'}
for mode in ['official','native']:
    d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
