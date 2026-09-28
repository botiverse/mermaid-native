import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class StateNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 static String strings(List<String> values){List<String> result=new ArrayList<>();for(String s:values)result.add(q(s));return "["+String.join(",",result)+"]";}
 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
 MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();
 if(!(diagram instanceof StateDiagram)){System.out.println("{\"error\":\"Another diagram type\"}");return;}
 StateDiagram d=(StateDiagram)diagram;List<String> nodes=new ArrayList<>(),edges=new ArrayList<>(),notes=new ArrayList<>();
 for(StateNode n:d.getStates())nodes.add("{\"id\":"+q(n.getId())+",\"classes\":"+strings(n.getClasses())+",\"styles\":"+strings(n.getStyles())+",\"label\":"+q(n.getLabel())+",\"kind\":"+q(n.getKind().name())+",\"explicitLabel\":"+n.getExplicitLabel()+",\"declared\":"+n.getDeclared()+",\"direction\":"+q(n.getDirection()==null?null:n.getDirection().name())+",\"description\":"+q(n.getDescription())+",\"children\":"+strings(n.getChildIds())+"}");
 for(StateTransition e:d.getTransitions())edges.add("{\"from\":"+q(e.getFrom())+",\"to\":"+q(e.getTo())+",\"label\":"+q(e.getLabel())+"}");
 for(StateNote n:d.getNotes())notes.add("{\"target\":"+q(n.getTargetId())+",\"position\":"+q(n.getPosition().name())+",\"text\":"+q(n.getText())+"}");
 List<String> defs=new ArrayList<>();for(var e:d.getClassDefinitions().entrySet())defs.add(q(e.getKey())+":"+strings(e.getValue()));
 System.out.println("{\"definitions\":{"+String.join(",",defs)+"},\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"direction\":"+q(d.getDirection().name())+",\"nodes\":["+String.join(",",nodes)+"],\"edges\":["+String.join(",",edges)+"],\"notes\":["+String.join(",",notes)+"]}");
 }
}
