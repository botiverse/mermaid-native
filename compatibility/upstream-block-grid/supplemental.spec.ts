import {it,expect,vi} from 'vitest';
import * as api from '../packages/mermaid/src/diagram-api/diagramAPI.js';
import {layout,calculateBlockPosition} from '../packages/mermaid/src/diagrams/block/layout.js';
const leaf=(id:string,w=100,h=50,span=1,type='square')=>({id,type,widthInColumns:span,children:[],size:{width:w,height:h,x:0,y:0}});
it('supplemental complete measured grids: spans spaces nesting auto columns and per-call padding',()=>{
 for(const cols of [-1,1,2,3])for(const p of [0,4,8,20]){
  for(const children of [
    [leaf('a'),leaf('b',200,80),leaf('c',50,40)],
    [leaf('a',230,70,2),leaf('s',999,999,3,'space'),leaf('b',70,30)],
    [{...leaf('g',400,10),columns:2,children:[leaf('a',120,30),leaf('b',40,120),leaf('c',90,80)]},leaf('d',600,200)],
    [{...leaf('g',0,0,2),columns:1,children:[{...leaf('h',0,0),columns:2,children:[leaf('a'),leaf('b',80,140)]},leaf('c')]},leaf('d',240,30)],
    [leaf('s',0,0,2,'space')],[],
  ]){
   vi.spyOn(api,'getConfig').mockReturnValue({block:{padding:p}} as any);
   const root={id:'root',type:'square',columns:cols,children};
   const result=layout({getBlock:(id:string)=>id==='root'?root:undefined} as any);
   expect(result).toBeDefined();expect(Number.isFinite(result!.width)).toBe(true);expect((root as any).size.height).toBeGreaterThanOrEqual(0);
  }
  expect(calculateBlockPosition(cols,7)).toBeDefined();
 }
 expect(layout({getBlock:()=>undefined} as any)).toBeUndefined();
});
