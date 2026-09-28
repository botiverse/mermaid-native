import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class WardleyNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}


 static Object get(Object o,String f){try{return o.getClass().getMethod(f).invoke(o);}catch(Exception e){return null;}}
 static String value(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){List<String>a=new ArrayList<>();for(var e:((Map<?,?>)o).entrySet())a.add(q(e.getKey().toString())+":"+value(e.getValue()));return "{"+String.join(",",a)+"}";}if(o instanceof Collection){List<String>a=new ArrayList<>();for(var x:(Collection<?>)o)a.add(value(x));return "["+String.join(",",a)+"]";}throw new IllegalArgumentException("Unsupported projection");}

 static List<?> list(Object o,String field){Object x=get(o,field);return x==null?List.of():(List<?>)x;}
 static Map<String,Object> point(Object n,boolean force){var m=new LinkedHashMap<String,Object>();m.put(force?"name":"text",get(n,"getText"));m.put("x",100*((Number)get(n,"getEvolution")).doubleValue());m.put("y",100*((Number)get(n,"getVisibility")).doubleValue());return m;}
 public static void main(String[] args){var input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){String source=decode(input.nextLine());try{
 var result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println(value(Map.of("error",((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())));continue;}
 var d=(WardleyMapDiagram)((MermaidParseResult.Success)result).getDiagram();var out=new LinkedHashMap<String,Object>();out.put("title",d.getTitle());var nodes=new ArrayList<>();
 for(var n:d.getNodes()){var m=new LinkedHashMap<String,Object>();String parent=(String)get(n,"getPipeline");m.put("id",parent==null?n.getName():parent+"_"+n.getName());m.put("label",n.getName());m.put("x",n.getEvolution()*100);m.put("y",n.getVisibility()*100);m.put("className",n.getAnchor()?"anchor":parent==null?null:"pipeline-component");for(String key:List.of("LabelOffsetX","LabelOffsetY","SourceStrategy","Inertia"))m.put(Character.toLowerCase(key.charAt(0))+key.substring(1),get(n,"get"+key));nodes.add(m);}
 out.put("nodes",nodes);var edges=new ArrayList<>();for(var e:d.getLinks()){var m=new LinkedHashMap<String,Object>();m.put("source",e.getFrom());m.put("target",e.getTo());m.put("flow",get(e,"getFlow"));m.put("label",get(e,"getLabel"));edges.add(m);}out.put("links",edges);
 out.put("axes",Map.of("stages",Objects.requireNonNullElse(get(d,"getStages"),List.of("Genesis","Custom Built","Product","Commodity")),"stageBoundaries",Objects.requireNonNullElse(get(d,"getStageBoundaries"),List.of())));
 var trends=new ArrayList<>();for(var e:d.getEvolutions())trends.add(Map.of("nodeId",e.getComponent(),"targetX",e.getEvolution()*100));out.put("trends",trends);
 out.put("notes",d.getNotes().stream().map(n->point(n,false)).toList());out.put("accelerators",list(d,"getAccelerators").stream().map(n->point(n,true)).toList());out.put("deaccelerators",list(d,"getDeaccelerators").stream().map(n->point(n,true)).toList());
 Object box=get(d,"getAnnotationsBox");if(box!=null)out.put("annotationsBox",Map.of("x",point(box,false).get("x"),"y",point(box,false).get("y")));
 var annotations=new ArrayList<>();for(var a:list(d,"getAnnotations")){var p=point(a,false);annotations.add(Map.of("text",p.get("text"),"coordinates",List.of(Map.of("x",p.get("x"),"y",p.get("y")))));}out.put("annotations",annotations);
 Object width=get(d,"getWidth"),height=get(d,"getHeight");if(width!=null&&height!=null)out.put("size",Map.of("width",width,"height",height));System.out.println(value(out));
 }catch(Throwable e){System.out.println(value(Map.of("error",e.getMessage()==null?e.getClass().getName():e.getMessage())));}}}
}
