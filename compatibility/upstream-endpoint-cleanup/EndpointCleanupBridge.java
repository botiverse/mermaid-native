import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;

/** Transport only: original points and node bounds go directly to the Native API. */
public class EndpointCleanupBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static double n(Map<String,Object> m,String k){Object v=m.get(k);return v instanceof Number?((Number)v).doubleValue():0;}
 static String s(Object o){return o==null?null:o.toString();}
 static List<ScenePoint> points(Object value){var out=new ArrayList<ScenePoint>();for(Object p:(List<?>)value){var m=map(p);out.add(new ScenePoint(n(m,"x"),n(m,"y")));}return out;}
 static List<Object> output(List<ScenePoint> points){var out=new ArrayList<Object>();for(var p:points)out.add(Map.of("x",p.getX(),"y",p.getY()));return out;}
 static Object invoke(Map<String,Object> row){var args=(List<?>)row.get("args");String op=s(row.get("op"));
  if(op.equals("orthogonalize"))return output(OrthogonalPolylineCleanup.INSTANCE.orthogonalize(points(args.get(0))));
  if(op.equals("simplify"))return output(OrthogonalPolylineCleanup.INSTANCE.simplify(points(args.get(0))));
  var routes=new ArrayList<OrthogonalEndpointRoute>();var rawEdges=(List<?>)args.get(0);
  for(Object e:rawEdges){var m=map(e);routes.add(new OrthogonalEndpointRoute(s(m.getOrDefault("id","")),points(m.getOrDefault("points",List.of())),s(m.get("start")),s(m.get("end")),Boolean.TRUE.equals(m.get("isLayoutOnly"))));}
  var nodes=new LinkedHashMap<String,SceneRect>();
  for(Object e:(List<?>)args.get(1)){var pair=(List<?>)e;var m=map(pair.get(1));double w=n(m,"width"),h=n(m,"height");nodes.put(s(pair.get(0)),new SceneRect(n(m,"x")-w/2,n(m,"y")-h/2,w,h));}
  var result=op.equals("clip")?OrthogonalEndpointCleanup.INSTANCE.clipToBoundaries(routes,nodes):OrthogonalEndpointCleanup.INSTANCE.prepareForRenderer(routes,nodes);
  // Preserve arbitrary original edge metadata; only the Native result's points replace input points.
  var out=new ArrayList<Object>();for(int i=0;i<result.size();i++){var m=new LinkedHashMap<String,Object>(map(rawEdges.get(i)));if(m.containsKey("points"))m.put("points",output(result.get(i).getPoints()));out.add(m);}return out;
 }
 public static void main(String[] args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
