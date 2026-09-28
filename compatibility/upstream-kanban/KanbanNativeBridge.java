import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class KanbanNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }



 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}");return;}
 KanbanDiagram d=(KanbanDiagram)((MermaidParseResult.Success)result).getDiagram();List<String> sections=new ArrayList<>(),nodes=new ArrayList<>();
 for(KanbanColumn c:d.getColumns()){
 String section="{\"id\":"+q(c.getId())+",\"label\":"+q(c.getTitle())+meta(c.getMetadata())+"}";
 sections.add(section);nodes.add(section);
 for(KanbanCard card:c.getCards())nodes.add("{\"id\":"+q(card.getId())+",\"label\":"+q(card.getLabel())+",\"parentId\":"+q(c.getId())+meta(card.getMetadata())+"}");
 }
 System.out.println("{\"sections\":["+String.join(",",sections)+"],\"nodes\":["+String.join(",",nodes)+"]}");
 }
 static String meta(KanbanMetadata m){return ",\"icon\":"+q(m.getIcon())+",\"cssClasses\":"+q(m.getCssClasses())+",\"assigned\":"+q(m.getAssigned())+",\"ticket\":"+q(m.getTicket())+",\"priority\":"+q(m.getPriority());}

}
