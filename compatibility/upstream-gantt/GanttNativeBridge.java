import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class GanttNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static String arr(Collection<?> items){return "["+String.join(",",items.stream().map(x->q(x.toString())).toList())+"]";}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
 MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();
 if(!(diagram instanceof GanttDiagram)){System.out.println("{\"error\":\"Another diagram type\"}");return;}
 GanttDiagram d=(GanttDiagram)diagram;List<String> sections=new ArrayList<>();
 for(GanttSection section:d.getSections()){
 List<String> tasks=new ArrayList<>();
 for(GanttTask t:section.getTasks()){
 String start=java.time.LocalDate.of(0,1,1).plusDays(t.getStartDay()).toString();
 String end=java.time.LocalDate.of(0,1,1).plusDays(t.getStartDay()+t.getDurationDays()).toString();
 tasks.add("{\"id\":"+q(t.getId())+",\"name\":"+q(t.getName())+",\"start\":"+q(start)+",\"end\":"+q(end)+",\"status\":"+q(t.getStatus().name())+",\"statuses\":"+arr(t.getStatuses())+",\"milestone\":"+t.getMilestone()+"}");
 }
 sections.add("{\"name\":"+q(section.getName())+",\"tasks\":["+String.join(",",tasks)+"]}");
 }
 List<String> actions=new ArrayList<>();for(FlowInteraction a:d.getInteractions())actions.add("{\"id\":"+q(a.getNodeId())+",\"value\":"+q(a.getValue())+",\"callback\":"+a.getCallback()+",\"args\":"+q(a.getArguments())+"}");
 System.out.println("{\"title\":"+q(d.getTitle())+",\"dateFormat\":"+q(d.getDateFormat())+",\"sections\":["+String.join(",",sections)+"],\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"excludes\":"+arr(d.getExcludes())+",\"includes\":"+arr(d.getIncludes())+",\"inclusive\":"+d.getInclusiveEndDates()+",\"today\":"+q(d.getTodayMarker())+",\"axis\":"+q(d.getAxisFormat())+",\"tick\":"+q(d.getTickInterval())+",\"weekday\":"+q(d.getWeekday())+",\"weekend\":"+q(d.getWeekend())+",\"actions\":["+String.join(",",actions)+"]}");
 }
}
