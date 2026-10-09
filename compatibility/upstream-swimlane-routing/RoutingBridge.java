import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;
public class RoutingBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static double d(Map<String,Object> m,String k,double fallback){return m.get(k)==null?fallback:((Number)m.get(k)).doubleValue();}
 static Object invoke(Map<String,Object> row){
  var args=(List<?>)row.get("args");var layout=map(args.get(0));var originals=(List<?>)layout.getOrDefault("nodes",List.of());
  var nodes=new LinkedHashMap<String,SceneRect>();var obstacles=new ArrayList<SceneRect>();
  for(var v:originals){var m=map(v);double w=d(m,"width",10),h=d(m,"height",10);var r=new SceneRect(d(m,"x",0)-w/2,d(m,"y",0)-h/2,w,h);nodes.put(m.get("id").toString(),r);if(!Boolean.TRUE.equals(m.get("isGroup"))&&!Boolean.TRUE.equals(m.get("isEdgeLabel")))obstacles.add(r);}
  var edges=(List<?>)layout.getOrDefault("edges",List.of());var previous=new ArrayList<List<ScenePoint>>();var updated=new ArrayList<Object>();
  for(var v:edges){var m=new LinkedHashMap<>(map(v));var a=nodes.get(m.get("start"));var b=nodes.get(m.get("end"));
   if(a!=null&&b!=null&&!Boolean.TRUE.equals(m.get("isLayoutOnly"))){var path=SwimlaneObstacleRoutesKt.swimlaneOrthogonalPath(a,b,obstacles,previous,true,true);if(path!=null){previous.add(path);var points=new ArrayList<Object>();for(var p:path)points.add(Map.of("x",p.getX(),"y",p.getY()));m.put("points",points);}}
   updated.add(m);
  }
  var out=new LinkedHashMap<>(layout);out.put("edges",updated);return out;
 }
 public static void main(String[]args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
