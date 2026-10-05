import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;

/** Transport only; preserve arbitrary original metadata around Native route and label results. */
public class RouteRefinementBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static double n(Map<String,Object> m,String k){Object v=m.get(k);return v instanceof Number?((Number)v).doubleValue():0;}
 static String s(Object o){return o==null?null:o.toString();}
 static List<ScenePoint> points(Object value){var out=new ArrayList<ScenePoint>();for(Object p:(List<?>)value){var m=map(p);out.add(new ScenePoint(n(m,"x"),n(m,"y")));}return out;}
 static List<Object> output(List<ScenePoint> points){var out=new ArrayList<Object>();for(var p:points)out.add(Map.of("x",p.getX(),"y",p.getY()));return out;}
 static Object invoke(Map<String,Object> row){var args=(List<?>)row.get("args");String op=s(row.get("op"));
  var routes=new ArrayList<OrthogonalRefinementRoute>();var rawEdges=(List<?>)args.get(0);var rawNodes=(List<?>)args.get(1);
  for(Object e:rawEdges){var m=map(e);routes.add(new OrthogonalRefinementRoute(s(m.getOrDefault("id","")),points(m.getOrDefault("points",List.of())),s(m.get("start")),s(m.get("end")),Boolean.TRUE.equals(m.get("isLayoutOnly")),s(m.get("labelNodeId"))));}
  var nodes=new ArrayList<OrthogonalGeometry.NodeBounds>();
  for(Object entry:rawNodes){var m=map(op.equals("swap")?entry:((List<?>)entry).get(1));nodes.add(OrthogonalGeometry.INSTANCE.nodeBoundsFromCenter(s(m.getOrDefault("id","")),n(m,"x"),n(m,"y"),n(m,"width"),n(m,"height"),Boolean.TRUE.equals(m.get("isGroup")),Boolean.TRUE.equals(m.get("isEdgeLabel"))));}
  List<OrthogonalRefinementRoute> result;List<OrthogonalGeometry.NodeBounds> nodeResult=nodes;
  if(op.equals("swap"))result=OrthogonalRouteRefinement.INSTANCE.swapPorts(routes,nodes);
  else if(op.equals("nudge"))result=OrthogonalRouteRefinement.INSTANCE.nudgeSharedTracks(routes,nodes,0.0);
  else {var value=OrthogonalRouteRefinement.INSTANCE.collapseTerminalStubs(routes,nodes);result=value.getRoutes();nodeResult=value.getNodes();}
  var edges=new ArrayList<Object>();for(int i=0;i<result.size();i++){var m=new LinkedHashMap<String,Object>(map(rawEdges.get(i)));if(m.containsKey("points"))m.put("points",output(result.get(i).getPoints()));edges.add(m);}
  var outputNodes=new ArrayList<Object>();for(int i=0;i<nodes.size();i++){Object entry=rawNodes.get(i);var m=new LinkedHashMap<String,Object>(map(op.equals("swap")?entry:((List<?>)entry).get(1)));var before=nodes.get(i).getRect();var after=nodeResult.get(i).getRect();if(!before.equals(after)){m.put("x",after.getX()+after.getWidth()/2);m.put("y",after.getY()+after.getHeight()/2);}outputNodes.add(op.equals("swap")?m:List.of(((List<?>)entry).get(0),m));}
  return Map.of("edges",edges,"nodes",outputNodes);
 }
 public static void main(String[] args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
