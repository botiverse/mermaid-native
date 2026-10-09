import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.FlowDirection;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;
public class DirectionBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static Double n(Map<String,Object> m,String k){return m.get(k)==null?null:((Number)m.get(k)).doubleValue();}
 static double d(Map<String,Object> m,String k){var v=n(m,k);return v==null?0:v;}
 static Object invoke(Map<String,Object> row){
  var args=(List<?>)row.get("args");var layout=map(args.get(0));var originals=(List<?>)layout.getOrDefault("nodes",List.of());
  var nodes=new ArrayList<SwimlaneDirectionGeometry.Node>();
  for(var v:originals){var m=map(v);SceneRect title=null;if(m.get("groupTitleRect")!=null){var t=map(m.get("groupTitleRect"));title=new SceneRect(d(t,"left"),d(t,"top"),d(t,"right")-d(t,"left"),d(t,"bottom")-d(t,"top"));}
   nodes.add(new SwimlaneDirectionGeometry.Node(m.get("id").toString(),n(m,"x"),n(m,"y"),d(m,"width"),d(m,"height"),Boolean.TRUE.equals(m.get("isGroup")),(String)m.get("parentId"),n(m,"padding"),title,n(m,"swimlaneContentTop")));
  }
  var edges=(List<?>)layout.getOrDefault("edges",List.of());var paths=new ArrayList<List<ScenePoint>>();
  for(var v:edges){var path=new ArrayList<ScenePoint>();for(var p:(List<?>)map(v).getOrDefault("points",List.of())){var m=map(p);path.add(new ScenePoint(d(m,"x"),d(m,"y")));}paths.add(path);}
  var direction=FlowDirection.valueOf(args.size()>1 && args.get(1)!=null?args.get(1).toString():"TB");
  var result=SwimlaneDirectionGeometry.INSTANCE.transform(nodes,paths,direction,36.0,false);var updated=new ArrayList<Object>();
  for(int i=0;i<nodes.size();i++){var old=nodes.get(i);var n=result.getNodes().get(i);var m=new LinkedHashMap<String,Object>(map(originals.get(i)));
   if(n.getX()!=null)m.put("x",n.getX());if(n.getY()!=null)m.put("y",n.getY());
   if(m.containsKey("width")||n.getWidth()!=old.getWidth())m.put("width",n.getWidth());if(m.containsKey("height")||n.getHeight()!=old.getHeight())m.put("height",n.getHeight());
   if(n.getTitle()!=null){var t=n.getTitle();m.put("groupTitleRect",Map.of("left",t.getX(),"right",t.getX()+t.getWidth(),"top",t.getY(),"bottom",t.getY()+t.getHeight()));}
   if(n.getContentTop()!=null)m.put("swimlaneContentTop",n.getContentTop());updated.add(m);
  }
  var updatedEdges=new ArrayList<Object>();for(int i=0;i<edges.size();i++){var m=new LinkedHashMap<String,Object>(map(edges.get(i)));if(m.containsKey("points")){var points=new ArrayList<Object>();for(var p:result.getPaths().get(i))points.add(Map.of("x",p.getX(),"y",p.getY()));m.put("points",points);}updatedEdges.add(m);}
  var out=new LinkedHashMap<>(layout);out.put("nodes",updated);out.put("edges",updatedEdges);return out;
 }
 public static void main(String[]args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
