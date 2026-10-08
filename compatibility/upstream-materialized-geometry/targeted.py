#!/usr/bin/env python3
"""Supplemental exact reference comparisons; not credited as upstream assertions."""
import base64,json,os,subprocess,sys
from pathlib import Path
w=Path(sys.argv[1]); cp=sys.argv[2]; u=w.parent
original=[json.loads(x) for x in (w/'official-calls.jsonl').read_text().splitlines()]
assert len(original)==11, "Run the complete original capture before supplemental comparisons"
cases=[]
# Repeat every captured operation under axis reversal/transposition and translated fractions.
for ci,c in enumerate(original):
 for t in range(4):
  row=json.loads(json.dumps({'op':c['op'],'args':c['args']}));row['name']=f'original-call-{ci}-transform-{t}'
  def point(p):
   x,y=p['x'],p['y']
   if t==0:x,y=y,x
   if t==1:x=-x
   if t==2:y=-y
   if t==3:x,y=x+0.0005,y-0.0005
   p.update(x=x,y=y)
  for e in row['args'][0]:
   for p in e.get('points',[]):point(p)
  for key,n in row['args'][1]:
   if 'x' in n and 'y' in n:point(n)
   if t==0:n['width'],n['height']=n.get('height'),n.get('width')
   r=n.get('groupTitleRect')
   if r:
    corners=[{'x':r['left'],'y':r['top']},{'x':r['right'],'y':r['bottom']}]
    for p in corners:point(p)
    r.update(left=min(p['x'] for p in corners),right=max(p['x'] for p in corners),top=min(p['y'] for p in corners),bottom=max(p['y'] for p in corners))
  cases.append(row)
# Crossing resolver has no direct assertion in the original file. Exercise actual crossings,
# identical edge IDs, hidden edges, labels, missing nodes and axis/scaling variants.
for variant in range(24):
 nodes=[[k,{'id':k,'x':x,'y':y,'width':20,'height':20}] for k,x,y in [('a',0,50),('b',200,50),('c',100,-50),('d',100,150)]]
 edges=[{'id':'duplicate','start':'a','end':'b','points':[{'x':10,'y':50},{'x':190,'y':50}]}, {'id':'duplicate','start':'c','end':'d','points':[{'x':100,'y':-40},{'x':100,'y':140}]}]
 if variant%6==1:edges.append({'id':'hidden','start':'a','end':'d','isLayoutOnly':True,'points':[{'x':10,'y':50},{'x':100,'y':50},{'x':100,'y':140}]})
 if variant%6==2:nodes.append(['obstacle',{'id':'obstacle','x':40,'y':0,'width':20,'height':30}])
 if variant%6==3:nodes.append(['label',{'id':'label','x':40,'y':0,'width':20,'height':30,'isEdgeLabel':True}])
 if variant%6==4:nodes=nodes[1:]
 if variant%6==5:edges[0]['points']=[{'x':10,'y':50},{'x':40,'y':50},{'x':40,'y':90},{'x':170,'y':90},{'x':170,'y':50},{'x':190,'y':50}]
 for e in edges:
  for p in e['points']:
   if variant>=12:p['x'],p['y']=p['y'],p['x']
   if variant%12>=6:p['x']=-p['x']
 for _,n in nodes:
  if variant>=12:n['x'],n['y'],n['width'],n['height']=n['y'],n['x'],n['height'],n['width']
  if variant%12>=6:n['x']=-n['x']
 cases.append({'op':'RESOLVE_CROSSINGS','args':[edges,nodes],'name':f'crossing-{variant}'})
(w/'targeted-input.json').write_text(json.dumps(cases))
methods=['separateSharedRenderedTerminalLanes','collapseRedundantRectangularDoglegs','liftObstacleHuggingSameSideRails','liftTopLaneTitleBandsAboveRails','shiftLeftLaneTitleBandsLeftOfRails','swapDestinationTerminalTailsToReduceCrossings','reassignCrossingExternalRailChannels','shortcutRedundantOrthogonalJogs','resolveRenderedOrthogonalCrossings']
ops=['SEPARATE_TERMINAL_LANES','COLLAPSE_DOGLEGS','LIFT_OBSTACLE_RAILS','LIFT_TOP_TITLES','SHIFT_LEFT_TITLES','SWAP_DESTINATION_TAILS','REASSIGN_EXTERNAL_CHANNELS','SHORTCUT_JOGS','RESOLVE_CROSSINGS']
(w/'targeted.spec.ts').write_text("import {test,expect} from 'vitest';import {readFileSync,writeFileSync} from 'node:fs';import * as m from '../packages/mermaid/src/rendering-util/layout-algorithms/swimlanes/direction/materializedGeometry.js';\n"+f"const methods={json.dumps(dict(zip(ops,methods)))};test('supplemental reference geometry',()=>{{const rows=JSON.parse(readFileSync({json.dumps(str(w/'targeted-input.json'))},'utf8'));const results=rows.map(row=>{{const [edges,ns]=row.args;const nodes=new Map(ns);m[methods[row.op]](edges,nodes);return {{edges,nodes:[...nodes]}};}});writeFileSync({json.dumps(str(w/'targeted-reference.json'))},JSON.stringify(results));expect(results.length).toBe(rows.length);}},120000);\n")
(w/'targeted.config.ts').write_text("import {defineConfig} from 'vitest/config';export default defineConfig({test:{environment:'node',globals:true,maxWorkers:1,include:['.native-materialized-geometry-audit/targeted.spec.ts']}});")
(w/'targeted.workspace.ts').write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace(['.native-materialized-geometry-audit/targeted.config.ts']);")
with (w/'targeted.log').open('w') as log:
 subprocess.run(['pnpm','exec','vitest','run','--config','.native-materialized-geometry-audit/targeted.config.ts','--workspace','.native-materialized-geometry-audit/targeted.workspace.ts'],cwd=u,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
(w/'targeted-native-input.txt').write_text('\n'.join(base64.b64encode(json.dumps(c).encode()).decode() for c in cases)+'\n')
with (w/'targeted-native-input.txt').open() as inp,(w/'targeted-native-output.jsonl').open('w') as out:
 subprocess.run(['java','-cp',cp,'MaterializedGeometryBridge'],stdin=inp,stdout=out,check=True,timeout=180)
actual=[json.loads(x) for x in (w/'targeted-native-output.jsonl').read_text().splitlines()]; expected=json.loads((w/'targeted-reference.json').read_text());assert len(actual)==len(expected)==len(cases)
(w/'targeted-native.json').write_text(json.dumps(actual))
failures=[{'name':c['name'],'expected':e,'actual':a} for c,e,a in zip(cases,expected,actual) if e!=a]
(w/'targeted-failures.json').write_text(json.dumps(failures,indent=2))
summary={'comparisons':len(cases),'passed':len(cases)-len(failures),'failed':len(failures),'resolverComparisons':24,'resolverChanged':sum(e!={'edges':c['args'][0],'nodes':c['args'][1]} for c,e in zip(cases,expected) if c['op']=='RESOLVE_CROSSINGS')}
(w/'targeted-summary.json').write_text(json.dumps(summary,indent=2));print(summary);assert not failures,[f['name'] for f in failures]
