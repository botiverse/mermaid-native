import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class CynefinNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}

 public static void main(String[] args){Scanner s=new Scanner(System.in,StandardCharsets.UTF_8);while(s.hasNextLine()){String input=s.nextLine();try{var parsed=MermaidParser.INSTANCE.parse(decode(input));
 if(parsed instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)parsed).getDiagnostics().get(0).getMessage())+"}");continue;}
 var d=(CynefinDiagram)((MermaidParseResult.Success)parsed).getDiagram();List<String> domains=new ArrayList<>(),edges=new ArrayList<>();
 for(var block:d.getDomains()){List<String> items=new ArrayList<>();for(var item:block.getItems())items.add("{\"label\":"+q(item)+"}");domains.add("{\"name\":"+q(block.getDomain().name().toLowerCase(Locale.ROOT))+",\"items\":["+String.join(",",items)+"]}");}
 for(var edge:d.getTransitions())edges.add("{\"from\":"+q(edge.getFrom().name().toLowerCase(Locale.ROOT))+",\"to\":"+q(edge.getTo().name().toLowerCase(Locale.ROOT))+",\"label\":"+q(edge.getLabel())+"}");
 System.out.println("{\"title\":"+q(d.getTitle())+",\"accTitle\":"+q(optional(d,"getAccTitle"))+",\"accDescr\":"+q(optional(d,"getAccDescription"))+",\"domains\":["+String.join(",",domains)+"],\"transitions\":["+String.join(",",edges)+"]}");
 }catch(Throwable e){System.out.println("{\"error\":"+q(e.getMessage()==null?e.getClass().getName():e.getMessage())+"}");}}}
 static String optional(Object value,String method)throws Exception{try{return (String)value.getClass().getMethod(method).invoke(value);}catch(NoSuchMethodException e){return null;}}
}
