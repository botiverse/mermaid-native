import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class ErNativeBridge {
 static String strings(List<String> xs) { List<String> out=new ArrayList<>(); for(String x:xs)out.add(q(x)); return "["+String.join(",",out)+"]"; }
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
   es.add("{\"id\":"+q(e.getId())+",\"alias\":"+q(e.getAlias()==null?"":e.getAlias())+",\"styles\":"+strings(e.getStyles())+",\"classes\":"+strings(e.getClasses())+",\"attributes\":["+String.join(",",as)+"]}");
  }
  for(EntityRelationship a:d.getRelationships()) rs.add("{\"from\":"+q(a.getFrom())+",\"to\":"+q(a.getTo())+",\"label\":"+q(a.getLabel())+",\"cardB\":"+q(a.getFromCardinality().name())+",\"cardA\":"+q(a.getToCardinality().name())+",\"relType\":"+q(a.getIdentifying()?"IDENTIFYING":"NON_IDENTIFYING")+"}");
  List<String> classes=new ArrayList<>();for(Map.Entry<String,List<String>> c:d.getClassDefinitions().entrySet())classes.add("["+q(c.getKey())+","+strings(c.getValue())+"]");
  List<String> groups=new ArrayList<>();for(EntitySubgraph g:d.getSubgraphs())groups.add("{\"id\":"+q(g.getId())+",\"title\":"+q(g.getTitle())+",\"nodeIds\":"+strings(g.getNodeIds())+",\"direction\":"+q(g.getDirection()==null?null:g.getDirection().name())+",\"styles\":"+strings(g.getStyles())+",\"classes\":"+strings(g.getClasses())+"}");
  System.out.println("{\"direction\":"+q(d.getDirection().name())+",\"subgraphs\":["+String.join(",",groups)+"],\"accessibilityTitle\":"+q(d.getAccessibilityTitle())+",\"accessibilityDescription\":"+q(d.getAccessibilityDescription())+",\"classDefinitions\":["+String.join(",",classes)+"],\"entities\":["+String.join(",",es)+"],\"relationships\":["+String.join(",",rs)+"]}");
 }
}
