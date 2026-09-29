import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class GanttCalendarOptionsNativeBridge {
 private static String quoted(String s) {return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
 private static String array(List<String> list) {return "["+String.join(",",list.stream().map(GanttCalendarOptionsNativeBridge::quoted).toList())+"]";}
 public static void main(String[] args) {
  Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);
  while(lines.hasNextLine()) {
   String source=new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8);
   MermaidParseResult parsed=MermaidParser.INSTANCE.parse(source);
   if(!(parsed instanceof MermaidParseResult.Success)) {System.out.println("{\"error\":\"Native parser rejected calendar options\"}");continue;}
   GanttDiagram diagram=(GanttDiagram)((MermaidParseResult.Success)parsed).getDiagram();
   System.out.println("{\"getExcludes\":"+array(diagram.getExcludes())+",\"getIncludes\":"+array(diagram.getIncludes())+"}");
  }
 }
}
