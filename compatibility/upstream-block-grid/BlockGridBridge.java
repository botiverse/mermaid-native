import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.simple.*;

/** Structural JSON transport; all grid arithmetic runs in production Kotlin. */
public class BlockGridBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static double n(Map<String,Object> m,String k,double fallback){return m.get(k)==null?fallback:((Number)m.get(k)).doubleValue();}
 static BlockGridLayout.Node node(Map<String,Object> m){
  var children=new ArrayList<BlockGridLayout.Node>();for(Object c:(List<?>)m.getOrDefault("children",List.of()))children.add(node(map(c)));
  BlockGridLayout.Size size=null;if(m.get("size")!=null){var s=map(m.get("size"));size=new BlockGridLayout.Size(n(s,"width",0),n(s,"height",0),n(s,"x",0),n(s,"y",0));}
  return new BlockGridLayout.Node(m.get("id").toString(),m.getOrDefault("type","square").toString(),(int)n(m,"columns",-1),(int)n(m,"widthInColumns",1),children,size,0.0);
 }
 static Object snapshot(BlockGridLayout.Node n){var out=new LinkedHashMap<String,Object>();out.put("id",n.getId());var s=n.getSize();if(s!=null)out.put("size",Map.of("width",s.getWidth(),"height",s.getHeight(),"x",s.getX(),"y",s.getY()));var children=new ArrayList<Object>();for(var c:n.getChildren())children.add(snapshot(c));out.put("children",children);return out;}
 static Object invoke(Map<String,Object> row){
  var args=(List<?>)row.get("args");
  if(row.get("op").equals("calculateBlockPosition")){var p=BlockGridLayout.INSTANCE.position(((Number)args.get(0)).intValue(),((Number)args.get(1)).intValue());return Map.of("px",p.getPx(),"py",p.getPy());}
  if(args.get(0)==null)return null;
  var root=node(map(args.get(0)));var b=BlockGridLayout.INSTANCE.layout(root,((Number)args.get(1)).doubleValue());
  return Map.of("bounds",Map.of("x",b.getX(),"y",b.getY(),"width",b.getWidth(),"height",b.getHeight()),"root",snapshot(root));
 }
 public static void main(String[] args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
