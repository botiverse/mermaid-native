import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class IshikawaNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}


 static Object get(Object o,String f){try{return o.getClass().getMethod(f).invoke(o);}catch(Exception e){return null;}}
 static String value(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){List<String>a=new ArrayList<>();for(var e:((Map<?,?>)o).entrySet())a.add(q(e.getKey().toString())+":"+value(e.getValue()));return "{"+String.join(",",a)+"}";}if(o instanceof Collection){List<String>a=new ArrayList<>();for(var x:(Collection<?>)o)a.add(value(x));return "["+String.join(",",a)+"]";}throw new IllegalArgumentException("Unsupported projection");}

 static Map<String,Object> node(IshikawaNode n){return Map.of("text",n.getText(),"children",n.getChildren().stream().map(IshikawaNativeBridge::node).toList());}
 public static void main(String[] args){var input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){String source=decode(input.nextLine());try{
 var result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println(value(Map.of("error",((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())));continue;}
 var d=(IshikawaDiagram)((MermaidParseResult.Success)result).getDiagram();System.out.println(value(Map.of("effect",node(d.getEffect()))));
 }catch(Throwable e){System.out.println(value(Map.of("error",e.getMessage()==null?e.getClass().getName():e.getMessage())));}}}
}
