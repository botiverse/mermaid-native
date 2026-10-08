import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;

/** JSON transport only; geometry is computed by the production Native implementation. */
public class BlockArrowBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static double n(Object o){return ((Number)o).doubleValue();}
 static Object invoke(Map<String,Object> row){
  var args=(List<?>)row.get("args"); var directions=new ArrayList<String>();
  for(Object d:(List<?>)args.get(0))directions.add(d.toString());
  var bbox=map(args.get(1));var node=map(args.get(2));
  var result=BlockArrowGeometry.INSTANCE.points(directions,new SceneSize(n(bbox.get("width")),n(bbox.get("height"))),node.get("padding")==null?0:n(node.get("padding")),args.size()<4||args.get(3)==null?null:n(args.get(3)));
  var out=new ArrayList<Object>();for(var p:result)out.add(Map.of("x",p.getX(),"y",p.getY()));return out;
 }
 public static void main(String[] args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
