import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class PieNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 public static void main(String[] args) {
  Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);
  while(lines.hasNextLine()) {
   String source=new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8);
   MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
   if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");continue;}
   PieDiagram d=(PieDiagram)((MermaidParseResult.Success)result).getDiagram();
   List<String> sections=new ArrayList<>();for(PieSection s:d.getSections())sections.add("{\"label\":"+q(s.getLabel())+",\"value\":"+s.getValue()+"}");
   System.out.println("{\"$type\":\"Pie\",\"title\":"+q(d.getTitle())+",\"showData\":"+d.getShowData()+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescr\":"+q(d.getAccessibilityDescription())+",\"sections\":["+String.join(",",sections)+"]}");
  }
 }
}
