import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class BlockNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}


 static Object get(Object o,String f){try{return o.getClass().getMethod(f).invoke(o);}catch(Exception e){return null;}}
 static String value(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){List<String>a=new ArrayList<>();for(var e:((Map<?,?>)o).entrySet())a.add(q(e.getKey().toString())+":"+value(e.getValue()));return "{"+String.join(",",a)+"}";}if(o instanceof Collection){List<String>a=new ArrayList<>();for(var x:(Collection<?>)o)a.add(value(x));return "["+String.join(",",a)+"]";}throw new IllegalArgumentException("Unsupported projection");}
 static Map<String,Object> node(BlockNode n){var m=new LinkedHashMap<String,Object>();m.put("id",n.getId());m.put("label",n.getLabel());m.put("widthInColumns",n.getColumnSpan());m.put("type",Objects.requireNonNullElse(get(n,"getType"),"square"));m.put("columns",Objects.requireNonNullElse(get(n,"getColumns"),-1));for(String f:List.of("Directions","Classes","Styles"))m.put(f.toLowerCase(),Objects.requireNonNullElse(get(n,"get"+f),List.of()));var children=new ArrayList<>();var raw=get(n,"getChildren");if(raw!=null)for(var x:(List<?>)raw)children.add(node((BlockNode)x));m.put("children",children);return m;}
 public static void main(String[] args){var input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){String source=decode(input.nextLine());try{
 var result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println(value(Map.of("error",((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())));continue;}
 var d=(BlockDiagram)((MermaidParseResult.Success)result).getDiagram();var out=new LinkedHashMap<String,Object>();out.put("columns",d.getColumns());out.put("nodes",d.getNodes().stream().map(BlockNativeBridge::node).toList());out.put("classes",Objects.requireNonNullElse(get(d,"getClasses"),Map.of()));out.put("warnings",Objects.requireNonNullElse(get(d,"getWarnings"),List.of()));var edges=new ArrayList<>();for(var e:d.getEdges()){var edge=new LinkedHashMap<String,Object>();edge.put("start",e.getFrom());edge.put("end",e.getTo());edge.put("arrowTypeEnd","arrow_point");edge.put("label",get(e,"getLabel"));edges.add(edge);}out.put("edges",edges);System.out.println(value(out));
 }catch(Throwable e){System.out.println(value(Map.of("error",e.getMessage()==null?e.getClass().getName():e.getMessage())));}}}
}
