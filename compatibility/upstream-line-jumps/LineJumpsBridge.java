import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;

/** Transport and DOM attributes only. Native implements all intersection, path and style changes. */
public class LineJumpsBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static String s(Object o){return o==null?null:o.toString();}
 static double n(Object o){return ((Number)o).doubleValue();}
 static List<ScenePoint> points(Object value){var out=new ArrayList<ScenePoint>();for(Object p:(List<?>)value){var m=map(p);out.add(new ScenePoint(n(m.get("x")),n(m.get("y"))));}return out;}
 static Object invoke(Map<String,Object> row){
  String op=s(row.get("op"));var args=(List<?>)row.get("args");var api=LineJumps.INSTANCE;
  if(op.equals("isStraightPath"))return api.isStraightPath(s(args.get(0)));
  if(op.equals("curveSupportsLineHops"))return api.curveSupportsLineHops(s(args.get(0)));
  var edges=new ArrayList<LineJumps.Edge>();
  for(Object e:(List<?>)args.get(0)){var m=map(e);edges.add(new LineJumps.Edge(s(m.get("id")),points(m.get("points")),s(m.get("curve")),s(m.get("arrowTypeStart")),s(m.get("arrowTypeEnd"))));}
  if(op.equals("findEdgeIntersections")){var out=new ArrayList<Object>();for(var c:api.findEdgeIntersections(edges))out.add(Map.of("jumpEdgeId",c.getJumpEdgeId(),"otherEdgeId",c.getOtherEdgeId(),"segIndex",c.getSegIndex(),"t",c.getT(),"point",Map.of("x",c.getPoint().getX(),"y",c.getPoint().getY())));return out;}
  var config=map(args.get(1));var cfg=new LineJumps.Config(Boolean.TRUE.equals(config.get("enabled")),n(config.get("jumpRadius")),"gap".equals(config.get("jumpStyle"))?LineJumps.Style.GAP:LineJumps.Style.ARC);
  if(op.equals("processEdgesWithJumps")){var out=new ArrayList<Object>();api.processEdgesWithJumps(edges,cfg).forEach((id,d)->out.add(List.of(id,d)));return out;}
  var paths=new ArrayList<LineJumps.RenderedPath>();var lengths=new HashMap<String,Double>();
  for(Object p:(List<?>)args.get(2)){var m=map(p);paths.add(new LineJumps.RenderedPath(s(m.get("id")),s(m.get("d")),s(m.get("dataPoints")),m.get("style")==null?"":s(m.get("style"))));if(m.get("length") instanceof Number)lengths.put(s(m.get("id")),n(m.get("length")));}
  var patches=api.patchRenderedPaths(edges,paths,cfg,(id,d)->lengths.get(id));var out=new ArrayList<Object>();
  for(var p:paths){var patch=patches.stream().filter(x->x.getId().equals(p.getId())).findFirst();var m=new LinkedHashMap<String,Object>();m.put("id",p.getId());m.put("d",patch.map(x->x.getGeometry().getD()).orElse(p.getD()));m.put("style",patch.map(LineJumps.Patch::getStyle).orElse(p.getStyle()));out.add(m);}return out;
 }
 public static void main(String[] args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));System.out.println(TreeDocumentsBridge.json(invoke(row)));}}
}
