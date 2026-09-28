import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class RequirementNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static String arr(Collection<?> xs){return "["+String.join(",",xs.stream().map(x->q(x.toString())).toList())+"]";}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
 MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();
 if(!(diagram instanceof RequirementDiagram)){System.out.println("{\"error\":\"Another diagram type\"}");return;}
 RequirementDiagram d=(RequirementDiagram)diagram;List<String> requirements=new ArrayList<>(),elements=new ArrayList<>(),edges=new ArrayList<>();
 for(RequirementDefinition r:d.getRequirements())requirements.add("{\"name\":"+q(r.getName())+",\"id\":"+q(r.getId())+",\"text\":"+q(r.getText())+",\"risk\":"+q(r.getRisk().name())+",\"verify\":"+q(r.getVerifyMethod().name())+",\"type\":"+q(r.getType().name())+",\"classes\":"+arr(r.getClasses())+",\"styles\":"+arr(r.getStyles())+"}");
 for(RequirementElement e:d.getElements())elements.add("{\"name\":"+q(e.getName())+",\"type\":"+q(e.getType())+",\"docref\":"+q(e.getDocRef())+",\"classes\":"+arr(e.getClasses())+",\"styles\":"+arr(e.getStyles())+"}");
 for(RequirementRelationship e:d.getRelationships())edges.add("{\"from\":"+q(e.getFrom())+",\"to\":"+q(e.getTo())+",\"kind\":"+q(e.getKind().name())+"}");
 List<String> defs=new ArrayList<>();for(var item:d.getClassDefinitions().entrySet())defs.add(q(item.getKey())+":"+arr(item.getValue()));
 System.out.println("{\"definitions\":{"+String.join(",",defs)+"},\"direction\":"+q(d.getDirection()==null?"TB":d.getDirection().name())+",\"requirements\":["+String.join(",",requirements)+"],\"elements\":["+String.join(",",elements)+"],\"edges\":["+String.join(",",edges)+"],\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+"}");
 }
}
