import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

public class GanttTaskModelNativeBridge {
 private static String quoted(String text) {return "\""+text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
 private static long millis(GanttTask task,String getter,long legacy) throws Exception {
  try {return (Long)task.getClass().getMethod(getter).invoke(task);}
  catch(NoSuchMethodException absentInPriorRuntime) {return legacy;}
 }
 public static void main(String[] args) throws Exception {
  Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);
  while(lines.hasNextLine()) {
   String text=new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8);
   MermaidParseResult parsed=MermaidParser.INSTANCE.parse(text);
   if(!(parsed instanceof MermaidParseResult.Success)) {System.out.println("{\"error\":\"Native production parser rejected diagram\"}");continue;}
   GanttDiagram diagram=(GanttDiagram)((MermaidParseResult.Success)parsed).getDiagram();
   List<String> tasks=new ArrayList<>();
   for(GanttSection section:diagram.getSections()) for(GanttTask task:section.getTasks()) {
    long start=millis(task,"getStartEpochMillis",(task.getStartDay()-719528L)*86400000L);
    long duration=millis(task,"getDurationMillis",task.getDurationDays()*86400000L);
    tasks.add("{\"id\":"+quoted(task.getId())+",\"task\":"+quoted(task.getName())+",\"startTime\":"+start+",\"endTime\":"+(start+duration)+"}");
   }
   System.out.println("["+String.join(",",tasks)+"]");
  }
 }
}
