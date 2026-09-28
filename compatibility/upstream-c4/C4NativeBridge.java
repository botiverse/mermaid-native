import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class C4NativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\"");break;case '\\':b.append("\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }




 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}");return;}
 C4Diagram d=(C4Diagram)((MermaidParseResult.Success)result).getDiagram();List<String> shapes=new ArrayList<>(),bounds=new ArrayList<>();
 bounds.add("{\"alias\":\"global\",\"label\":{\"text\":\"global\"},\"type\":{\"text\":\"global\"},\"tags\":null,\"link\":null,\"parentBoundary\":\"\"}");
 for(C4Element n:d.getElements()){
 String type=(n.getExternal()?"external_":"")+n.getKind().name().toLowerCase()+switch(n.getVariant()){case "database"->"_db";case "queue"->"_queue";default->"";};
 String tech=(n.getKind()==C4ElementKind.CONTAINER||n.getKind()==C4ElementKind.COMPONENT)?",\"techn\":{\"text\":"+q(n.getTechnology()==null?"":n.getTechnology())+"}":"";
 shapes.add("{\"alias\":"+q(n.getId())+",\"label\":{\"text\":"+value(n.getLabel(),n.getLabelAttribute())+"},\"descr\":{\"text\":"+q(n.getDescription()==null?"":n.getDescription())+"},\"typeC4Shape\":{\"text\":"+q(type)+"},\"parentBoundary\":"+q(n.getParentBoundary())+",\"wrap\":false"+tech+attrs(n.getAttributes())+"}");
 }
 for(C4Boundary n:d.getBoundaries())bounds.add("{\"alias\":"+q(n.getId())+",\"label\":{\"text\":"+value(n.getLabel(),n.getLabelAttribute())+"},\"type\":{\"text\":"+q(n.getType())+"},\"parentBoundary\":"+q(n.getParentBoundary())+",\"wrap\":false"+attrs(n.getAttributes())+"}");
 System.out.println("{\"title\":"+q(d.getTitle())+",\"shapes\":["+String.join(",",shapes)+"],\"boundaries\":["+String.join(",",bounds)+"]}");
 }
 static String value(String text,String key){return key==null?q(text):"{"+q(key)+":"+q(text)+"}";}
 static String attrs(Map<String,String> m){String out="";for(var e:m.entrySet())out+=","+q(e.getKey())+":"+q(e.getValue());return out;}
}
