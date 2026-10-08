import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;

/** JSON transport only: all geometry and metadata mutations come from Native. */
public class MaterializedGeometryBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static Double n(Map<String,Object> m,String k){Object v=m.get(k);return v instanceof Number?((Number)v).doubleValue():null;}
 static String s(Object o){return o==null?null:o.toString();}
 static List<ScenePoint> points(Object value){if(value==null)return null;var out=new ArrayList<ScenePoint>();for(Object p:(List<?>)value){var m=map(p);out.add(new ScenePoint(n(m,"x"),n(m,"y")));}return out;}
 static List<Object> output(List<ScenePoint> points){var out=new ArrayList<Object>();for(var p:points)out.add(Map.of("x",p.getX(),"y",p.getY()));return out;}
 static MaterializedGeometry.Bounds bounds(Object value){if(!(value instanceof Map))return null;var r=map(value);for(String k:List.of("left","right","top","bottom"))if(n(r,k)==null)return null;return new MaterializedGeometry.Bounds(n(r,"left"),n(r,"right"),n(r,"top"),n(r,"bottom"));}
 static MaterializedGeometry.Node node(Map<String,Object> m){return new MaterializedGeometry.Node(s(m.get("id")),n(m,"x"),n(m,"y"),n(m,"width"),n(m,"height"),Boolean.TRUE.equals(m.get("isGroup")),Boolean.TRUE.equals(m.get("isEdgeLabel")),s(m.get("parentId")),s(m.get("direction")),bounds(m.get("groupTitleRect")));}
 static void changed(Map<String,Object> out,String key,Object before,Object after){if(!Objects.equals(before,after))out.put(key,after);}
 static Object invoke(Map<String,Object> row){
  var args=(List<?>)row.get("args");var rawEdges=(List<?>)args.get(0);var rawNodes=(List<?>)args.get(1);
  var edges=new ArrayList<MaterializedGeometry.Edge>();var nodes=new LinkedHashMap<String,MaterializedGeometry.Node>();
  for(Object e:rawEdges){var m=map(e);edges.add(new MaterializedGeometry.Edge(s(m.get("id")),s(m.get("start")),s(m.get("end")),points(m.get("points")),Boolean.TRUE.equals(m.get("isLayoutOnly"))));}
  for(Object e:rawNodes){var pair=(List<?>)e;nodes.put(s(pair.get(0)),node(map(pair.get(1))));}
  var result=MaterializedGeometry.INSTANCE.apply(MaterializedGeometry.Operation.valueOf(s(row.get("op"))),edges,nodes);
  var outEdges=new ArrayList<Object>();for(int i=0;i<edges.size();i++){var m=new LinkedHashMap<String,Object>(map(rawEdges.get(i)));var p=result.getEdges().get(i).getPoints();if(p!=null)m.put("points",output(p));outEdges.add(m);}
  var outNodes=new ArrayList<Object>();for(Object e:rawNodes){var pair=(List<?>)e;String key=s(pair.get(0));var m=new LinkedHashMap<String,Object>(map(pair.get(1)));var before=nodes.get(key);var after=result.getNodes().get(key);
   changed(m,"x",before.getX(),after.getX());changed(m,"y",before.getY(),after.getY());changed(m,"width",before.getWidth(),after.getWidth());changed(m,"height",before.getHeight(),after.getHeight());
   if(!Objects.equals(before.getGroupTitleRect(),after.getGroupTitleRect())){var r=after.getGroupTitleRect();m.put("groupTitleRect",Map.of("left",r.getLeft(),"right",r.getRight(),"top",r.getTop(),"bottom",r.getBottom()));}
   outNodes.add(List.of(pair.get(0),m));
  }
  return Map.of("edges",outEdges,"nodes",outNodes);
 }
 public static void main(String[] args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
