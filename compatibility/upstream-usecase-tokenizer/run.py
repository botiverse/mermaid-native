#!/usr/bin/env python3
"""Run all unchanged Usecase lexer assertions against production Kotlin tokens."""
import argparse,base64,hashlib,json,os,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--upstream',type=Path,required=True);p.add_argument('--stdlib',type=Path,required=True);p.add_argument('--verify-mutations',action='store_true');o=p.parse_args()
h=Path(__file__).resolve().parent;r=h.parents[1];u=o.upstream.resolve();w=u/'.native-usecase-tokenizer-audit';w.mkdir(exist_ok=True)
m=json.loads((h/'sources.json').read_text())
for f,sha in m['files'].items():
 for path in (u/f,r/'compatibility/upstream-batch/originals'/f):assert hashlib.sha256(path.read_bytes()).hexdigest()==sha,path
for f,sha in m['dependencies'].items():assert hashlib.sha256((u/f).read_bytes()).hexdigest()==sha,f
calls=w/'official-calls.jsonl';calls.write_text('');extra_calls=w/'extra-calls.jsonl';extra_calls.write_text('')
shared="""import {vi,expect,afterEach,afterAll} from 'vitest';
import {usecaseLexer} from '../packages/mermaid/src/diagrams/usecase/parser/usecase.lexer.js';
const name=()=>expect.getState().currentTestName??'';
"""
(w/'capture.ts').write_text(shared+"""import {appendFileSync} from 'node:fs';
const original=usecaseLexer.tokenize;
vi.spyOn(usecaseLexer,'tokenize').mockImplementation(function(source){
 const result=original.call(usecaseLexer,source);
 const tokens=result.tokens.map(t=>({name:t.tokenType.name,image:t.image,startOffset:t.startOffset,endOffset:t.endOffset,startLine:t.startLine,startColumn:t.startColumn,endLine:t.endLine,endColumn:t.endColumn}));
 const errors=result.errors.map(e=>({offset:e.offset,length:e.length,line:e.line,column:e.column}));
 appendFileSync(CALLS,JSON.stringify({test:name(),source,reference:{tokens,errors}})+'\\n');return result;
});
afterAll(()=>{for(const source of EXTRA_INPUTS){const result=original.call(usecaseLexer,source);
 const tokens=result.tokens.map(t=>({name:t.tokenType.name,image:t.image,startOffset:t.startOffset,endOffset:t.endOffset,startLine:t.startLine,startColumn:t.startColumn,endLine:t.endLine,endColumn:t.endColumn}));
 const errors=result.errors.map(e=>({offset:e.offset,length:e.length,line:e.line,column:e.column}));
 appendFileSync(EXTRA_CALLS,JSON.stringify({test:'additional differential',source,reference:{tokens,errors}})+'\\n');
}});
""".replace('EXTRA_INPUTS',json.dumps([x if isinstance(x,str) else x.get('prefix','')+x['repeat']*x['count']+x.get('suffix','') for x in json.loads((h/'edge-cases.json').read_text())])).replace('EXTRA_CALLS',json.dumps(str(extra_calls))).replace('CALLS',json.dumps(str(calls))))
config="""import {defineConfig} from 'vitest/config';
export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,testTimeout:30000,include:FILES,setupFiles:['.native-usecase-tokenizer-audit/SETUP.ts']}});
""".replace('FILES',json.dumps(list(m['files'])))
for mode,setup in [('official','capture'),('native','adapter')]:
 (w/f'{mode}.config.ts').write_text(config.replace('SETUP',setup));(w/f'{mode}.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-usecase-tokenizer-audit/"+mode+".config.ts']);")
def test(mode):
 with (w/f'{mode}.log').open('w') as f:return subprocess.run(['pnpm','exec','vitest','run','--config',f'.native-usecase-tokenizer-audit/{mode}.config.ts','--workspace',f'.native-usecase-tokenizer-audit/{mode}.workspace.ts','--reporter=json',f'--outputFile={w/(mode+".json")}'],cwd=u,stdout=f,stderr=subprocess.STDOUT,timeout=240)
assert test('official').returncode==0,'Reference failed';assert json.loads((w/'official.json').read_text())['numPassedTests']==13
jar=w/'native-runtime.jar';shutil.copy2(r/'mermaid-core/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar',jar);cp=os.pathsep.join(map(str,[w,jar,o.stdlib.resolve()]))
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'UsecaseLexerNativeBridge.java')],check=True)
trace=[json.loads(x) for x in calls.read_text().splitlines()];extras=[json.loads(x) for x in extra_calls.read_text().splitlines()]
result=subprocess.run(['java','-cp',cp,'UsecaseLexerNativeBridge'],input='\n'.join(base64.b64encode(x['source'].encode()).decode() for x in trace+extras)+'\n',text=True,capture_output=True,check=True,timeout=60)
all_snapshots=[json.loads(x) for x in result.stdout.splitlines()];assert len(all_snapshots)==len(trace)+len(extras);snapshots=all_snapshots[:len(trace)]
cache=w/'native-snapshots.json';cache.write_text(json.dumps([{**call,'snapshot':snap} for call,snap in zip(trace,snapshots)]))
# Exact differential comparison also checks every source location the assertions do not inspect.
differences=[{'test':call['test'],'source':call['source'],'reference':call['reference'],'native':snap} for call,snap in zip(trace+extras,all_snapshots) if call['reference']!=snap]
(w/'differential.json').write_text(json.dumps(differences,indent=2));assert not differences,f'{len(differences)} token/position differences; see differential.json'
(w/'adapter.ts').write_text(shared+"""import {readFileSync} from 'node:fs';
const rows=JSON.parse(readFileSync(CACHE,'utf8'));const byTest=new Map();for(const row of rows){if(!byTest.has(row.test))byTest.set(row.test,[]);byTest.get(row.test).push(row);}
vi.spyOn(usecaseLexer,'tokenize').mockImplementation(source=>{
 const row=byTest.get(name())?.shift();if(!row||row.source!==source)throw new Error('Native lexer trace mismatch: '+name());
 return {tokens:row.snapshot.tokens.map(({name,...token})=>({...token,tokenType:{name}})),errors:row.snapshot.errors,groups:{}};
});
afterEach(()=>{if(byTest.get(name())?.length)throw new Error('Unconsumed Native calls: '+name());});
""".replace('CACHE',json.dumps(str(cache))))
status=test('native')
def failed():return {a['fullName'] for s in json.loads((w/'native.json').read_text())['testResults'] for a in s['assertionResults'] if a['status']=='failed'}
baseline=failed();summary={'upstreamRevision':m['revision'],'nativeSource':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'nativeJarSha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'nativeTokenizeCalls':len(trace),'additionalDifferentialCases':len(extras),'exactReferenceTokenComparisons':sum(len(s['tokens']) for s in snapshots)}
for mode in ['official','native']:
 d=json.loads((w/f'{mode}.json').read_text());summary[mode]={k:d[k] for k in ['numTotalTests','numPassedTests','numFailedTests','numPendingTests','success']}
if o.verify_mutations:
 original=cache.read_text();controls={}
 try:
  for key in ['operator','keyword','newline','jsonLocation','unclosedMarkdown','decimal']:
   mutated=json.loads(original);targets=set();changed=0
   for row in mutated:
    source=row['source'];tokens=row['snapshot']['tokens'];name=row['test']
    if key=='operator' and source.startswith('--|>'):tokens[0]['name']='MARKERLESS_SOLID'
    elif key=='keyword' and source.startswith('actor actorName'):tokens[1]['name']='ACTOR'
    elif key=='newline' and source=='\r\n\r\n':tokens.pop()
    elif key=='jsonLocation' and name.endswith('balances nested JSON while ignoring escaped quotes and braces in strings'):tokens[1]['startColumn']+=1
    elif key=='unclosedMarkdown' and source=='"`x'*32000:tokens[0]['name']='MARKDOWN_STRING'
    elif key=='decimal' and source=='1.5 1.5px .5':tokens[0]['name']='IDENTIFIER'
    else:continue
    changed+=1;targets.add(name.replace(' > ',' '))
   assert changed==1 and len(targets)==1,(key,targets)
   cache.write_text(json.dumps(mutated));test('native');assert failed()-baseline==targets,(key,failed(),targets)
   controls[key]=sorted(targets);shutil.copy2(w/'native.json',w/f'mutation-{key}.json')
 finally:cache.write_text(original);status=test('native');assert failed()==baseline
 summary['mutationControls']=controls
(w/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2));raise SystemExit(status.returncode)
