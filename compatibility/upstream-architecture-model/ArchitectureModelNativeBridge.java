import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

public class ArchitectureModelNativeBridge {
 static String q(String s) { if(s==null)return "null";return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\""; }
 static Object get(Object x,String field) throws Exception {return x.getClass().getMethod(field).invoke(x);}
 static List<?> optionalList(Object x,String field) throws Exception {try{return (List<?>)get(x,field);}catch(NoSuchMethodException old){return List.of();}}
 public static void main(String[] args) throws Exception {
  Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);
  while(input.hasNextLine()){
   var parsed=MermaidParser.INSTANCE.parse(new String(Base64.getDecoder().decode(input.nextLine()),StandardCharsets.UTF_8));
   if(parsed instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)parsed).getDiagnostics().get(0).getMessage())+"}");continue;}
   var d=(ArchitectureDiagram)((MermaidParseResult.Success)parsed).getDiagram();
   List<String> services=new ArrayList<>(),groups=new ArrayList<>(),junctions=new ArrayList<>(),hints=new ArrayList<>();
   for(var s:d.getServices())services.add("{\"id\":"+q(s.getId())+",\"title\":"+q(s.getLabel())+",\"in\":"+q(s.getGroupId())+"}");
   for(var g:d.getGroups())groups.add("{\"id\":"+q(g.getId())+",\"title\":"+q(g.getLabel())+"}");
   for(var j:optionalList(d,"getJunctions"))junctions.add("{\"id\":"+q((String)get(j,"getId"))+",\"in\":"+q((String)get(j,"getGroupId"))+"}");
   for(var h:optionalList(d,"getLayoutHints")){
    List<String> members=new ArrayList<>();for(var member:(List<?>)get(h,"getMembers"))members.add(q((String)member));
    hints.add("{\"direction\":"+q((String)get(h,"getDirection"))+",\"members\":["+String.join(",",members)+"]}");
   }
   System.out.println("{\"getDiagramTitle\":"+q(d.getTitle()==null?"":d.getTitle())+",\"getAccTitle\":"+q(d.getAccTitle()==null?"":d.getAccTitle())+",\"getAccDescription\":"+q(d.getAccDescription()==null?"":d.getAccDescription())+",\"getServices\":["+String.join(",",services)+"],\"getGroups\":["+String.join(",",groups)+"],\"getJunctions\":["+String.join(",",junctions)+"],\"getLayoutHints\":["+String.join(",",hints)+"]}");
  }
 }
}
