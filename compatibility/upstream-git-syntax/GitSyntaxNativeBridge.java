import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class GitSyntaxNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static Object get(Object o,String method)throws Exception{return o.getClass().getMethod(method).invoke(o);}
 static String arr(Collection<?> values){List<String> v=new ArrayList<>();for(Object s:values)v.add(q(String.valueOf(s)));return "["+String.join(",",v)+"]";}
 public static void main(String[] args){Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){String source=new String(Base64.getDecoder().decode(input.nextLine()),StandardCharsets.UTF_8);try{
 Class<?> parser;
 try{parser=Class.forName("build.raft.mermaid.core.GitGraphSyntaxParser");}catch(ClassNotFoundException missing){var result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Success){var d=(GitGraphDiagram)((MermaidParseResult.Success)result).getDiagram();System.out.println("{\"value\":{\"statements\":[],\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescr\":"+q(d.getAccessibilityDescription())+"},\"lexerErrors\":[],\"parserErrors\":[]}");}else System.out.println("{\"value\":{\"statements\":[]},\"lexerErrors\":[],\"parserErrors\":[\"No syntax tree in baseline; semantic parser rejected input\"]}");continue;}
 Object parsed=parser.getMethod("parse",String.class).invoke(parser.getField("INSTANCE").get(null),source);List<String> statements=new ArrayList<>();
 for(Object st:(Collection<?>)get(parsed,"getStatements")){
 List<String> fields=new ArrayList<>();fields.add("\"$type\":"+q((String)get(st,"getType")));String[] names={"id","message","name","branch","parent"};
 for(String n:names){Object v=get(st,"get"+Character.toUpperCase(n.charAt(0))+n.substring(1));if(v!=null)fields.add(q(n)+":"+q(v.toString()));}
 Object kind=get(st,"getCommitType");if(kind!=null)fields.add("\"type\":"+q(kind.toString()));
 Object order=get(st,"getOrder");if(order!=null)fields.add("\"order\":"+order);
 fields.add("\"tags\":"+arr((Collection<?>)get(st,"getTags")));statements.add("{"+String.join(",",fields)+"}");}
 System.out.println("{\"value\":{\"statements\":["+String.join(",",statements)+"],\"accTitle\":"+q((String)get(parsed,"getAccTitle"))+",\"accDescr\":"+q((String)get(parsed,"getAccDescription"))+"},\"lexerErrors\":[],\"parserErrors\":"+arr((Collection<?>)get(parsed,"getDiagnostics"))+"}");
 }catch(Throwable e){System.out.println("{\"value\":{\"statements\":[]},\"lexerErrors\":[],\"parserErrors\":["+q(e.toString())+"]}");}}}
}
