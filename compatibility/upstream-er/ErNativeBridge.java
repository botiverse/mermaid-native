import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class ErNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 public static void main(String[] args) throws Exception {
  if(args.length>0 && args[0].equals("--batch")) {
   Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);
   while(lines.hasNextLine()) render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));
  } else render(new String(System.in.readAllBytes(),StandardCharsets.UTF_8));
 }
 static void render(String source) {
  MermaidParseResult r=MermaidParser.INSTANCE.parse(source);
  if(r instanceof MermaidParseResult.Failure) {System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)r).getDiagnostics().toString())+"}");return;}
  EntityRelationshipDiagram d=(EntityRelationshipDiagram)((MermaidParseResult.Success)r).getDiagram();
  List<String> es=new ArrayList<>(),rs=new ArrayList<>();
  for(EntityDefinition e:d.getEntities()){
   List<String> as=new ArrayList<>();for(EntityAttribute a:e.getAttributes()){
    List<String> ks=new ArrayList<>();if(a.getKey()!=EntityKey.NONE)ks.add(q(a.getKey().name()));for(EntityKey k:a.getAdditionalKeys())ks.add(q(k.name()));
    as.add("{\"type\":"+q(a.getType())+",\"name\":"+q(a.getName())+",\"comment\":"+q(a.getComment()==null?"":a.getComment())+",\"keys\":["+String.join(",",ks)+"]}");
   }
   es.add("{\"id\":"+q(e.getId())+",\"alias\":"+q(e.getAlias()==null?"":e.getAlias())+",\"attributes\":["+String.join(",",as)+"]}");
  }
  for(EntityRelationship a:d.getRelationships()) rs.add("{\"from\":"+q(a.getFrom())+",\"to\":"+q(a.getTo())+",\"label\":"+q(a.getLabel())+",\"cardB\":"+q(a.getFromCardinality().name())+",\"cardA\":"+q(a.getToCardinality().name())+",\"relType\":"+q(a.getIdentifying()?"IDENTIFYING":"NON_IDENTIFYING")+"}");
  System.out.println("{\"entities\":["+String.join(",",es)+"],\"relationships\":["+String.join(",",rs)+"]}");
 }
}
