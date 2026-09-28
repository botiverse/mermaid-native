import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class TimelineNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
 MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();if(!(diagram instanceof TimelineDiagram)){System.out.println("{\"error\":\"Another diagram type\"}");return;}TimelineDiagram d=(TimelineDiagram)diagram;List<String> events=new ArrayList<>();
 for(TimelineEvent e:d.getEvents()){List<String> labels=new ArrayList<>();for(String label:e.getLabels())labels.add(q(label));events.add("{\"period\":"+q(e.getPeriod())+",\"section\":"+q(e.getSection())+",\"sectionIndex\":"+e.getSectionIndex()+",\"labels\":["+String.join(",",labels)+"]}");}
 List<String> sections=new ArrayList<>();for(String section:d.getSections())sections.add(q(section));
 System.out.println("{\"direction\":"+q(d.getDirection().name().equals("TB")?"TD":"LR")+",\"sections\":["+String.join(",",sections)+"],\"title\":"+q(d.getTitle())+",\"events\":["+String.join(",",events)+"]}");
 }
}
