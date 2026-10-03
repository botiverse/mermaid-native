import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Direct document calls; this bridge never parses or manufactures Mermaid source. */
public class JourneyDocumentBridge {
 public static void main(String[] args) {
  JourneyDocument doc=new JourneyDocument();Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);
  while(input.hasNextLine()){
   String[] fields=input.nextLine().split("\\t",-1);String op=fields[0];String[] a=new String[fields.length-1];
   for(int i=1;i<fields.length;i++)a[i-1]=new String(Base64.getDecoder().decode(fields[i]),StandardCharsets.UTF_8);
   Object value=null;
   try{switch(op){
    case "clear":doc.clear();break;
    case "addSection":doc.addSection(a[0]);break;
    case "addTask":doc.addTask(a[0],a[1]);break;
    case "setDiagramTitle":doc.setTitle(a[0]);break;
    case "getDiagramTitle":value=doc.getTitle();break;
    case "setAccTitle":doc.setAccessibilityTitle(a[0]);break;
    case "getAccTitle":value=doc.getAccessibilityTitle();break;
    case "setAccDescription":doc.setAccessibilityDescription(a[0]);break;
    case "getAccDescription":value=doc.getAccessibilityDescription();break;
    case "getSections":value=doc.sections();break;
    case "getActors":value=doc.actors();break;
    case "getTasks":List<Object> tasks=new ArrayList<>();for(JourneyDocumentTask t:doc.tasks())tasks.add(Map.of("task",t.getDescription(),"score",t.getScore(),"people",t.getPeople(),"section",t.getSection(),"type",t.getSection()));value=tasks;break;
    default:throw new IllegalArgumentException("Unsupported operation "+op);
   }System.out.println(TreeDocumentsBridge.json(Collections.singletonMap("value",value)));}
   catch(Exception e){System.out.println(TreeDocumentsBridge.json(Map.of("error",e.getMessage())));}
  }
 }
}
