import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class SequenceNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }

 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
  MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
  if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
  SequenceDiagram d=(SequenceDiagram)((MermaidParseResult.Success)result).getDiagram();

  List<String> actors=new ArrayList<>(),events=new ArrayList<>();
  for(SequenceActor a:d.getActors())actors.add("{\"id\":"+q(a.getId())+",\"label\":"+q(a.getLabel())+",\"kind\":"+q(a.getKind().name())+",\"wrap\":"+a.getWrap()+"}");
  for(SequenceEvent event:d.getEvents()){
   String value;
   if(event instanceof SequenceMessage){SequenceMessage m=(SequenceMessage)event;value="{\"kind\":\"message\",\"from\":"+q(m.getFrom())+",\"to\":"+q(m.getTo())+",\"label\":"+q(m.getLabel())+",\"style\":"+q(m.getLineStyle().name())+",\"head\":"+q(m.getArrowHead().name())+",\"wrap\":"+m.getWrap()+",\"bidirectional\":"+m.getBidirectional()+",\"central\":"+q(m.getCentralConnection().name())+",\"activate\":"+m.getActivate()+"}";}
   else if(event instanceof SequenceNote){SequenceNote n=(SequenceNote)event;List<String> ids=new ArrayList<>();for(String id:n.getActorIds())ids.add(q(id));value="{\"kind\":\"note\",\"position\":"+q(n.getPosition().name())+",\"actors\":["+String.join(",",ids)+"],\"text\":"+q(n.getText())+",\"wrap\":"+n.getWrap()+"}";}
   else if(event instanceof SequenceActivation){SequenceActivation a=(SequenceActivation)event;value="{\"kind\":\"activation\",\"actor\":"+q(a.getActorId())+",\"activate\":"+a.getActivate()+"}";}
   else if(event instanceof SequenceNumbering){SequenceNumbering n=(SequenceNumbering)event;value="{\"kind\":\"numbering\",\"visible\":"+n.getVisible()+",\"start\":"+n.getStart()+",\"step\":"+n.getStep()+"}";}
   else {SequenceFragment f=(SequenceFragment)event;value="{\"kind\":\"fragment\",\"fragment\":"+q(f.getKind().name())+",\"boundary\":"+q(f.getBoundary().name())+",\"label\":"+q(f.getLabel())+",\"wrap\":"+f.getWrap()+"}";}
   events.add(value);
  }
  System.out.println("{\"title\":"+q(d.getTitle())+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"actors\":["+String.join(",",actors)+"],\"events\":["+String.join(",",events)+"]}");
 }
}
