import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class EventGrammarNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}

 static Object optional(Object value,String method,Object fallback)throws Exception{try{return value.getClass().getMethod(method).invoke(value);}catch(NoSuchMethodException e){return fallback;}}
 static String array(Collection<?> values){List<String> result=new ArrayList<>();for(Object value:values)result.add(q(String.valueOf(value)));return "["+String.join(",",result)+"]";}
 public static void main(String[] args){Scanner s=new Scanner(System.in,StandardCharsets.UTF_8);while(s.hasNextLine()){try{
 String[] parts=s.nextLine().split("\t",-1);
 if(parts[0].equals("validate")){
 List<String> sources=parts[2].isEmpty()?List.of():Arrays.asList(parts[2].split(","));List<String> errors;
 try{Class<?> v=Class.forName("build.raft.mermaid.core.EventModelingValidator");errors=(List<String>)v.getMethod("errors",String.class,List.class).invoke(v.getField("INSTANCE").get(null),parts[1],sources);}
 catch(ClassNotFoundException e){StringBuilder text=new StringBuilder("eventmodeling\n");for(int i=0;i<sources.size();i++)text.append("tf ").append(i).append(" ").append(sources.get(i)).append(" Source").append(i).append("\n");text.append("tf 99 ").append(parts[1]).append(" Target");for(int i=0;i<sources.size();i++)text.append(" ->> ").append(i);var parsed=MermaidParser.INSTANCE.parse(text.toString());errors=parsed instanceof MermaidParseResult.Failure?List.of(((MermaidParseResult.Failure)parsed).getDiagnostics().get(0).getMessage()):List.of();}
 System.out.println("{\"errors\":"+array(errors)+"}");continue;
 }
 var parsed=MermaidParser.INSTANCE.parse(decode(parts[1]));if(parsed instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)parsed).getDiagnostics().get(0).getMessage())+"}");continue;}
 var d=(EventModelingDiagram)((MermaidParseResult.Success)parsed).getDiagram();List<String> frames=new ArrayList<>();
 for(var f:d.getFrames()){List<String> refs=new ArrayList<>();for(var edge:d.getRelations())if(edge.getTargetFrameId().equals(f.getId()))refs.add(edge.getSourceFrameId());String type=(String)optional(f,"getSourceType",f.getKind().name().toLowerCase(Locale.ROOT).replace("read_model","readmodel"));frames.add("{\"$type\":"+q(f.getReset()?"EmResetFrame":"EmTimeFrame")+",\"name\":"+q(f.getId())+",\"entityIdentifier\":"+q(f.getEntityId())+",\"modelEntityType\":"+q(type)+",\"sourceFrames\":"+array(refs)+"}");}
 System.out.println("{\"frames\":["+String.join(",",frames)+"],\"dataEntities\":"+array(d.getData().keySet())+",\"noteEntities\":"+array((Collection<?>)optional(d,"getNotes",List.of()))+",\"gwtEntities\":"+array((Collection<?>)optional(d,"getScenarios",List.of()))+"}");
 }catch(Throwable e){System.out.println("{\"error\":"+q(e.getMessage()==null?e.getClass().getName():e.getMessage())+"}");}}}
}
