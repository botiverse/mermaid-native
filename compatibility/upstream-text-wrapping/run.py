#!/usr/bin/env python3
"""Replay original grapheme/code-point and measured-line wrapping assertions through Kotlin."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--runtime-jar',type=Path,required=True);o=p.parse_args()
u=o.upstream.resolve();h=Path(__file__).resolve().parent;w=u/'.native-text-wrapping-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
jar=w/'native-runtime.jar';shutil.copy2(o.runtime_jar,jar);cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'TextWrappingNativeBridge.java'),str(h.parent/'upstream-railroad/RailroadNativeBridge.java')],check=True)
calls=w/'official-calls.jsonl';calls.write_text('')
shared="""import {vi,expect} from 'vitest';
import * as text from '../packages/mermaid/src/rendering-util/splitText.js';
const mode=()=>Boolean(Intl.Segmenter);
// The pinned tests supply a monospace character-count checkFit callback.
// Probe its constraint; actual segmentation, counting and wrapping run in Kotlin.
function widthOf(fit){for(let n=0;n<=1024;n++)if(!fit([{content:'x'.repeat(n),type:'normal'}]))return n-1;throw new Error('Unsupported unbounded fixture measurer');}
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
function record(input){appendFileSync(CALLS,JSON.stringify({test:expect.getState().currentTestName,input})+'\\n');}
const split=text.splitTextToChars,wrap=text.splitLineToFitWidth;
vi.spyOn(text,'splitTextToChars').mockImplementation(value=>{record({op:'split',mode:mode(),text:value});return split(value);});
vi.spyOn(text,'splitLineToFitWidth').mockImplementation((words,fit)=>{const width=widthOf(fit);record({op:'wrap',mode:mode(),words:structuredClone(words),width});return wrap(words,fit);});
""".replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
export default defineConfig({test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-text-wrapping-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-text-wrapping-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as log:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-text-wrapping-audit/{mode}.config.ts','--workspace',f'.native-text-wrapping-audit/{mode}.workspace.ts','--testNamePattern','^(?!.*should create valid checkFit function).*$','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=log,stderr=subprocess.STDOUT,timeout=240)
if test('official').returncode:raise SystemExit('Original text-wrapping suite failed')
inputs=list({json.dumps(json.loads(line)['input'],ensure_ascii=False):json.loads(line)['input'] for line in calls.read_text().splitlines()}.values())
def encode(value):return base64.b64encode(value.encode()).decode()
def request(item):
 fields=[item['op'],'1' if item['mode'] else '0']
 if item['op']=='split':fields.append(encode(item['text']))
 else:
  fields.extend([str(item['width']),str(len(item['words']))])
  for word in item['words']:fields.extend([encode(word['content']),encode(word['type'])])
 return '\t'.join(fields)
result=subprocess.run(['java','-cp',cp,'TextWrappingNativeBridge'],input=''.join(request(item)+'\n' for item in inputs),text=True,capture_output=True,check=True,timeout=120)
models=[json.loads(line) for line in result.stdout.splitlines()];assert len(models)==len(inputs)
cache=w/'native-models.json';cache.write_text(json.dumps(list(zip(inputs,models)),ensure_ascii=False))
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const results=new Map(JSON.parse(readFileSync(CACHE,'utf8')).map(([input,value])=>[JSON.stringify(input),value]));
function native(input){const key=JSON.stringify(input);if(!results.has(key))throw new Error('Missing Native input '+key);const value=structuredClone(results.get(key));if(value.error)throw new Error(value.error);return value;}
vi.spyOn(text,'splitTextToChars').mockImplementation(value=>native({op:'split',mode:mode(),text:value}));
vi.spyOn(text,'splitLineToFitWidth').mockImplementation((words,fit)=>{const width=widthOf(fit);return native({op:'wrap',mode:mode(),words,width});});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native');summary={'upstreamRevision':m['revision'],'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'uniqueInputs':len(inputs),'scope':'Original word segmentation supplies input/expected tokens; not claimed as Native segmentation coverage. One test-helper-only assertion is unselected.'}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
