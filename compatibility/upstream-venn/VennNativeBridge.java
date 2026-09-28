import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class VennNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}


 static String arr(List<String>a){List<String>b=new ArrayList<>(a);Collections.sort(b);return "["+String.join(",",b.stream().map(VennNativeBridge::q).toList())+"]";}
 static Object get(Object o,String field){try{return o.getClass().getMethod(field).invoke(o);}catch(Exception e){return null;}}
 static List<?> list(Object o,String field){Object a=get(o,field);return a==null?List.of():(List<?>)a;}
 public static void main(String[] args){Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){String source=decode(input.nextLine());try{
 var result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}");continue;}
 var d=(VennDiagram)((MermaidParseResult.Success)result).getDiagram();List<String> subsets=new ArrayList<>(),texts=new ArrayList<>(),styles=new ArrayList<>();
 for(var s:d.getSets())subsets.add("{\"sets\":"+arr(List.of(s.getId()))+",\"size\":"+(s.getSize()==null?10:s.getSize())+",\"label\":"+q(s.getLabel())+"}");
 for(var s:d.getUnions())subsets.add("{\"sets\":"+arr(s.getSetIds())+",\"size\":"+(s.getSize()==null?10.0/(s.getSetIds().size()*s.getSetIds().size()):s.getSize())+(s.getLabel()==null?"":",\"label\":"+q(s.getLabel()))+"}");
 for(var t:list(d,"getTexts"))texts.add("{\"sets\":"+arr((List<String>)get(t,"getSetIds"))+",\"id\":"+q((String)get(t,"getId"))+(get(t,"getLabel")==null?"":",\"label\":"+q((String)get(t,"getLabel")))+"}");
 for(var s:list(d,"getStyles")){List<String>props=new ArrayList<>();for(var e:((Map<String,String>)get(s,"getProperties")).entrySet())props.add(q(e.getKey())+":"+q(e.getValue()));styles.add("{\"targets\":"+arr((List<String>)get(s,"getTargets"))+",\"styles\":{"+String.join(",",props)+"}}");}
 System.out.println("{\"title\":"+q(d.getTitle())+",\"subsets\":["+String.join(",",subsets)+"],\"texts\":["+String.join(",",texts)+"],\"styles\":["+String.join(",",styles)+"]}");
 }catch(Throwable e){System.out.println("{\"error\":"+q(e.getMessage())+"}");}}}
}
