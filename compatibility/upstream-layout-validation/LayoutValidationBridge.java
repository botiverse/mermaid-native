import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;

/** Transport only: pass original measured geometry directly to the production validator. */
public class LayoutValidationBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static String str(Map<String,Object> m,String k){return UsecaseDocumentBridge.str(m,k,null);}
 static double num(Map<String,Object> m,String k,double fallback){Object o=m.get(k);return o instanceof Number?((Number)o).doubleValue():fallback;}
 static boolean bool(Map<String,Object> m,String k){return Boolean.TRUE.equals(m.get(k));}
 static SceneRect centered(Map<String,Object> m){double w=num(m,"width",40),h=num(m,"height",40);return new SceneRect(num(m,"x",0)-w/2,num(m,"y",0)-h/2,w,h);}
 static SceneRect title(Object o){if(o==null)return null;var m=map(o);double l=num(m,"left",Double.NaN),r=num(m,"right",Double.NaN),t=num(m,"top",Double.NaN),b=num(m,"bottom",Double.NaN);return new SceneRect(l,t,r-l,b-t);}
 static LayoutValidationInput input(Map<String,Object> m){
  var nodes=new ArrayList<LayoutValidationNode>();var edges=new ArrayList<LayoutValidationEdge>();
  for(Object raw:(List<?>)m.getOrDefault("nodes",List.of())){var n=map(raw);if(n.get("id")==null)continue;nodes.add(new LayoutValidationNode(str(n,"id"),centered(n),str(n,"parentId"),bool(n,"isGroup"),bool(n,"isEdgeLabel"),bool(n,"isDummy"),str(n,"shape"),title(n.get("groupTitleRect"))));}
  for(Object raw:(List<?>)m.getOrDefault("edges",List.of())){
   var e=map(raw);var points=new ArrayList<ScenePoint>();for(Object p:(List<?>)e.getOrDefault("points",List.of())){var v=map(p);points.add(new ScenePoint(num(v,"x",Double.NaN),num(v,"y",Double.NaN)));}
   SceneRect label=e.containsKey("x")&&e.containsKey("y")&&e.containsKey("width")&&e.containsKey("height")?centered(e):null;
   edges.add(new LayoutValidationEdge(UsecaseDocumentBridge.str(e,"id",""),points,str(e,"start"),str(e,"end"),str(e,"labelNodeId"),str(e,"label"),label,str(e,"arrowTypeStart"),str(e,"arrowTypeEnd"),str(e,"type")));
  }return new LayoutValidationInput(nodes,edges);
 }
 static Object value(Object o){
  if(o instanceof ScenePoint){var p=(ScenePoint)o;return Map.of("x",p.getX(),"y",p.getY());}
  if(o instanceof SceneRect){var r=(SceneRect)o;return Map.of("left",r.getX(),"right",r.getX()+r.getWidth(),"top",r.getY(),"bottom",r.getY()+r.getHeight(),"cx",r.getX()+r.getWidth()/2,"cy",r.getY()+r.getHeight()/2);}
  if(o instanceof Map){var out=new LinkedHashMap<String,Object>();map(o).forEach((k,v)->out.put(k,value(v)));return out;}
  if(o instanceof Iterable){var out=new ArrayList<>();for(Object v:(Iterable<?>)o)out.add(value(v));return out;}return o;
 }
 static Object result(LayoutValidationResult r){
  var issues=new ArrayList<>();for(var issue:r.getIssues()){var row=new LinkedHashMap<String,Object>();row.put("type",issue.getType());row.put("message",issue.getMessage());row.put("nodeIds",issue.getNodeIds());row.put("edgeId",issue.getEdgeId());row.put("details",value(issue.getDetails()));issues.add(row);}
  var b=r.getBreakdown();var penalties=new ArrayList<>();for(var e:b.getEdges())penalties.add(Map.of("id",e.getId(),"points",e.getPoints(),"bendPenalty",e.getBendPenalty()));
  return Map.of("ok",r.getOk(),"score",r.getScore(),"issues",issues,"breakdown",Map.of("nodeCount",b.getNodeCount(),"edgeCount",b.getEdgeCount(),"crossings",b.getCrossings(),"totalPoints",b.getTotalPoints(),"totalBendPenalty",b.getTotalBendPenalty(),"crossingPenalty",b.getCrossingPenalty(),"edges",penalties,"pointsHistogram",b.getPointsHistogram()));
 }
 public static void main(String[]args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){String raw=new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8);var input=input(map(UsecaseDocumentBridge.parse(raw)));System.out.println(TreeDocumentsBridge.json(result(OrthogonalLayoutValidator.INSTANCE.validate(input))));}}
}
