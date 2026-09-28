import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class MindmapNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }




 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}");return;}
 MindmapDiagram d=(MindmapDiagram)((MermaidParseResult.Success)result).getDiagram();List<String> nodes=new ArrayList<>();
 for(MindmapNode n:d.getNodes()){
 int type=switch(n.getShape().name()){case "RECTANGLE"->2;case "DOUBLE_CIRCLE"->3;case "ROUNDED_RECTANGLE"->1;case "CLOUD"->4;case "BANG"->5;case "HEXAGON"->6;default->0;};
 nodes.add("{\"id\":"+q(n.getId())+",\"nodeId\":"+q(n.getSourceId())+",\"descr\":"+q(n.getLabel())+",\"parentId\":"+q(n.getParentId())+",\"type\":"+type+",\"icon\":"+q(n.getIcon())+",\"class\":"+q(n.getCssClasses())+"}");
 }
 System.out.println("{\"nodes\":["+String.join(",",nodes)+"]}");
 }
}
