#!/usr/bin/env python3
"""Check the candidate-dedup equivalence classes against actual JS toFixed(3)."""
import json,math,subprocess,sys
from pathlib import Path
h=Path(__file__).resolve().parent;w=Path(sys.argv[1]);cp=sys.argv[2]
values=[0.0,-0.0,5e-324,-5e-324,1e20,1e20+16384,1e21,1e21+131072]
for i in range(-250,251):
 x=i/1000+0.0005;values.extend([x,math.nextafter(x,-math.inf),math.nextafter(x,math.inf),round(x,3)])
subprocess.run(['javac','-cp',cp,'-d',str(w),str(h/'FixedKeyBridge.java')],check=True)
(w/'decimal-key-input.txt').write_text('\n'.join(repr(v) for v in values)+'\n')
with (w/'decimal-key-input.txt').open() as inp,(w/'decimal-key-native.txt').open('w') as out:
 subprocess.run(['java','-cp',cp,'FixedKeyBridge'],stdin=inp,stdout=out,check=True,timeout=60)
actual=(w/'decimal-key-native.txt').read_text().splitlines()
(w/'decimal-key-values.json').write_text(json.dumps(values))
with (w/'decimal-key-reference.json').open('w') as out:
 subprocess.run(['node','-e',"const fs=require('fs');process.stdout.write(JSON.stringify(JSON.parse(fs.readFileSync(process.argv[1],'utf8')).map(v=>v.toFixed(3))));",str(w/'decimal-key-values.json')],stdout=out,check=True,timeout=60)
expected=json.loads((w/'decimal-key-reference.json').read_text())
def partition(keys):
 seen={};return [seen.setdefault(k,i) for i,k in enumerate(keys)]
assert partition(actual)==partition(expected)
(w/'decimal-keys-summary.json').write_text(json.dumps({'inputs':len(values),'equivalenceClasses':len(set(expected)),'passed':True},indent=2));print('JS toFixed(3) Native dedup classes match:',len(values),'inputs')
