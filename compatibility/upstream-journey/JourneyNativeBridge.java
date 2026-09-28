import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class JourneyNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
 MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();if(!(diagram instanceof UserJourneyDiagram)){System.out.println("{\"error\":\"Another diagram type\"}");return;}UserJourneyDiagram d=(UserJourneyDiagram)diagram;List<String> sections=new ArrayList<>();
 for(UserJourneySection section:d.getSections()){List<String> tasks=new ArrayList<>();for(UserJourneyTask t:section.getTasks()){List<String> actors=new ArrayList<>();for(String actor:t.getActors())actors.add(q(actor));tasks.add("{\"label\":"+q(t.getLabel())+",\"score\":"+t.getScore()+",\"actors\":["+String.join(",",actors)+"]}");}sections.add("{\"name\":"+q(section.getName())+",\"tasks\":["+String.join(",",tasks)+"]}");}
 System.out.println("{\"title\":"+q(d.getTitle())+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"sections\":["+String.join(",",sections)+"]}");
 }
}
