import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class UsecaseModelNativeBridge {
 static String q(String s) { StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String json(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Boolean||o instanceof Number)return o.toString();if(o instanceof Enum)return q(o.toString().toLowerCase(Locale.ROOT));if(o instanceof Map){List<String> rows=new ArrayList<>();((Map<?,?>)o).forEach((k,v)->rows.add(q(k.toString())+":"+json(v)));return "{"+String.join(",",rows)+"}";}if(o instanceof Iterable){List<String> rows=new ArrayList<>();for(Object v:(Iterable<?>)o)rows.add(json(v));return "["+String.join(",",rows)+"]";}throw new IllegalArgumentException(o.getClass().getName());}
 static Object getter(Object o,String name,Object fallback){try{return o.getClass().getMethod(name).invoke(o);}catch(ReflectiveOperationException e){return fallback;}}
 static Map<String,Object> row(Object o){Map<String,Object> r=new LinkedHashMap<>();r.put("id",getter(o,"getId",null));r.put("label",getter(o,"getLabel",null));return r;}
 static Map<String,Object> render(String source){MermaidParseResult result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure)return Map.of("error",((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage());UsecaseDiagram d=(UsecaseDiagram)((MermaidParseResult.Success)result).getDiagram();Map<String,Object> out=new LinkedHashMap<>();out.put("diagramType","usecase");out.put("direction",d.getDirection().name());List<Object> actors=new ArrayList<>();for(UsecaseActor a:d.getActors()){Map<String,Object> r=row(a);r.put("type",getter(a,"getType","normal"));r.put("icon",getter(a,"getIcon",null));r.put("business",getter(a,"getBusiness",false));actors.add(r);}out.put("actors",actors);List<Object> nodes=new ArrayList<>();for(UsecaseNode n:d.getUseCases()) {
  Map<String,Object> r=row(n);List<String> styles=new ArrayList<>();
  UsecaseAttributes attributes=d.getAttributes().get(n.getId());
  if(attributes!=null)for(var entry:attributes.getStyles().entrySet())styles.add(entry.getKey()+":"+entry.getValue());
  r.put("styles",styles);r.put("shape",n.getShape()==UsecaseShape.RECTANGLE?"rect":"ellipse");nodes.add(r);
}out.put("useCases",nodes);
List<Object> jsonNodes=new ArrayList<>();for(UsecaseJsonNode n:d.getJsonNodes())jsonNodes.add(Map.of("id",n.getId()));out.put("jsonNodes",jsonNodes);List<Object> boundaries=new ArrayList<>();for(UsecaseBoundary n:d.getBoundaries()) {
  Map<String,Object> r=row(n);
  r.put("labelType",n.getLabelType());
  UsecaseAttributes attributes=d.getAttributes().get(n.getId());
  if(attributes!=null)r.put("type",attributes.getProperties().get("type"));
  r.put("classes",attributes==null?List.of():attributes.getClasses());
  List<String> styles=new ArrayList<>();
  if(attributes!=null)for(var entry:attributes.getStyles().entrySet())styles.add(entry.getKey()+":"+entry.getValue());
  r.put("styles",styles);
  // Project the Native parent links in their production insertion order. No source parsing.
  List<String> members=new ArrayList<>();
  for(var entry:d.getAttributes().entrySet())
    if(n.getId().equals(entry.getValue().getParentId()))members.add(entry.getKey());
  r.put("members",members);boundaries.add(r);
}out.put("boundaries",boundaries);List<Object> edges=new ArrayList<>();for(UsecaseRelationship e:d.getRelationships()){Map<String,Object> r=row(e);r.put("source",e.getSourceId());r.put("target",e.getTargetId());r.put("type",getter(e,"getType",null));edges.add(r);}out.put("relationships",edges);Map<String,Object> classDefs=new LinkedHashMap<>();
for(var definition:d.getClassDefs().entrySet()) {
 List<String> styles=new ArrayList<>();
 for(var entry:definition.getValue().entrySet())styles.add(entry.getKey()+":"+entry.getValue());
 classDefs.put(definition.getKey(),Map.of("styles",styles));
}
out.put("classDefs",classDefs);out.put("noteCount",d.getNotes().size());return out;}
 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine()){String source=new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8);try{System.out.println(json(render(source)));}catch(Throwable e){System.out.println(json(Map.of("error",e.toString())));}}}
}
