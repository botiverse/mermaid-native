import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

public class GanttDurationNativeBridge {
 private static String quoted(String text) {return "\""+text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
 public static void main(String[] args) throws Exception {
  Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);
  while(lines.hasNextLine()) {
   String text=new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8);
   if(args.length>0) {System.out.println(MermaidParser.INSTANCE.parse(text) instanceof MermaidParseResult.Success?"accepted":"rejected");continue;}
   if(text.startsWith("gantt\n")) {
    MermaidParseResult parsed=MermaidParser.INSTANCE.parse(text);
    if(!(parsed instanceof MermaidParseResult.Success)) {System.out.println("{\"error\":\"Native production parser rejected diagram\"}");continue;}
    GanttDiagram diagram=(GanttDiagram)((MermaidParseResult.Success)parsed).getDiagram();
    List<String> tasks=new ArrayList<>();
    for(GanttSection section:diagram.getSections()) for(GanttTask task:section.getTasks()) {
     long start=(Long)task.getClass().getMethod("getStartEpochMillis").invoke(task);
     long duration=(Long)task.getClass().getMethod("getDurationMillis").invoke(task);
     tasks.add("{\"id\":"+quoted(task.getId())+",\"task\":"+quoted(task.getName())+",\"startTime\":"+start+",\"endTime\":"+(start+duration)+"}");
    }
    System.out.println("["+String.join(",",tasks)+"]");continue;
   }
   try {
    Class<?> c=Class.forName("build.raft.mermaid.core.GanttDuration");
    Object result=c.getMethod("parse",String.class).invoke(c.getField("INSTANCE").get(null),text);
    double value=(Double)result.getClass().getMethod("getAmount").invoke(result);
    String unit=(String)result.getClass().getMethod("getUnit").invoke(result);
    System.out.println("["+(Double.isNaN(value)?"\"NaN\"":Double.toString(value))+","+quoted(unit)+"]");
   } catch(ReflectiveOperationException e) {System.out.println("{\"error\":\"Native duration boundary unavailable\"}");}
  }
 }
}
