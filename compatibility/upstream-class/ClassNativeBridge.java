import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class ClassNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }

 static String strings(List<String> values){List<String> result=new ArrayList<>();for(String s:values)result.add(q(s));return "["+String.join(",",result)+"]";}
 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
  MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
  if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
  ClassDiagram d=(ClassDiagram)((MermaidParseResult.Success)result).getDiagram();
  List<String> classes=new ArrayList<>(),relations=new ArrayList<>();
  for(ClassDefinition c:d.getClasses()){
   List<String> members=new ArrayList<>();for(ClassMember m:c.getMembers())members.add("{\"signature\":"+q(m.getSignature())+",\"visibility\":"+q(m.getVisibility().name())+",\"hasVisibility\":"+m.getHasVisibility()+"}");
   classes.add("{\"id\":"+q(c.getId())+",\"label\":"+q(c.getLabel())+",\"namespace\":"+q(c.getNamespaceName())+",\"generic\":"+q(c.getGenericType())+",\"cssClasses\":"+strings(c.getClasses())+",\"styles\":"+strings(c.getStyles())+",\"annotations\":"+strings(c.getAnnotations())+",\"members\":["+String.join(",",members)+"]}");
  }
  for(ClassRelationship r:d.getRelationships())relations.add("{\"from\":"+q(r.getFrom())+",\"to\":"+q(r.getTo())+",\"kind\":"+q(r.getKind().name())+",\"fromMarker\":"+q(r.getFromMarker()==null?null:r.getFromMarker().name())+",\"toMarker\":"+q(r.getToMarker()==null?null:r.getToMarker().name())+",\"dashed\":"+r.getDashed()+",\"label\":"+q(r.getLabel())+",\"fromCardinality\":"+q(r.getFromCardinality())+",\"toCardinality\":"+q(r.getToCardinality())+"}");
  List<String> interactions=new ArrayList<>();for(ClassInteraction i:d.getInteractions())interactions.add("{\"classId\":"+q(i.getClassId())+",\"value\":"+q(i.getValue())+",\"callback\":"+i.getCallback()+",\"arguments\":"+q(i.getArguments())+",\"tooltip\":"+q(i.getTooltip())+",\"target\":"+q(i.getTarget())+"}");
  List<String> defs=new ArrayList<>();for(Map.Entry<String,List<String>> e:d.getClassDefinitions().entrySet())defs.add(q(e.getKey())+":"+strings(e.getValue()));
  List<String> notes=new ArrayList<>();for(ClassNote n:d.getNotes())notes.add("{\"text\":"+q(n.getText())+",\"classId\":"+q(n.getClassId())+",\"namespace\":"+q(n.getNamespaceName())+"}");
  List<String> ns=new ArrayList<>();for(ClassNamespace n:d.getNamespaces())ns.add("{\"id\":"+q(n.getId())+",\"label\":"+q(n.getLabel())+",\"explicit\":"+n.getExplicit()+"}");
  System.out.println("{\"interactions\":["+String.join(",",interactions)+"],\"definitions\":{"+String.join(",",defs)+"},\"notes\":["+String.join(",",notes)+"],\"direction\":"+q(d.getDirection().name())+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"namespaces\":["+String.join(",",ns)+"],\"classes\":["+String.join(",",classes)+"],\"relationships\":["+String.join(",",relations)+"]}");
 }
}
