import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import build.raft.mermaid.core.*;

public class UsecaseJsonNativeBridge {
  static String q(String s) {
    if(s==null)return "null";
    StringBuilder b=new StringBuilder("\"");
    for(char c:s.toCharArray())switch(c){
      case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;
      case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;
      default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);
    }
    return b.append('"').toString();
  }
  static Object get(Object x,String name)throws Exception{return x.getClass().getMethod(name).invoke(x);}
  static String json(Object x)throws Exception{
    if(x==null)return "null";
    if(x instanceof String)return q((String)x);
    if(x instanceof Number||x instanceof Boolean)return x.toString();
    if(x instanceof Map<?,?>){List<String>a=new ArrayList<>();for(var e:((Map<?,?>)x).entrySet())a.add(q(e.getKey().toString())+":"+json(e.getValue()));return "{"+String.join(",",a)+"}";}
    if(x instanceof Iterable<?>){List<String>a=new ArrayList<>();for(Object v:(Iterable<?>)x)a.add(json(v));return "["+String.join(",",a)+"]";}
    if(x.getClass().getSimpleName().equals("NullValue"))return "null";
    return json(get(x,"getValue"));
  }
  static String dec(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
  public static void main(String[] args){Scanner in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine())try{
    String[] p=in.nextLine().split("\t",-1);String source=dec(p[0]);int line=Integer.parseInt(dec(p[1])),column=Integer.parseInt(dec(p[2]));
    try{
      Class<?> c=Class.forName("build.raft.mermaid.core.UsecaseJsonParser");
      Object result=c.getMethod("parseOrderedJsonObject",String.class,int.class,int.class).invoke(c.getField("INSTANCE").get(null),source,line,column);
      System.out.println("{\"value\":{\"value\":"+json(get(result,"getValue"))+",\"propertyOrder\":"+json(get(result,"getPropertyOrder"))+"}}");
    }catch(ClassNotFoundException missing){
      var parsed=MermaidParser.INSTANCE.parse("usecase-beta\njson probe@"+source);
      if(parsed instanceof MermaidParseResult.Success){var d=(UsecaseDiagram)((MermaidParseResult.Success)parsed).getDiagram();System.out.println("{\"value\":{\"unconvertedSource\":"+q(d.getJsonNodes().get(0).getSource())+"},\"baselineMissingOrderedJsonBoundary\":true}");}
      else {var e=((MermaidParseResult.Failure)parsed).getDiagnostics().get(0);System.out.println("{\"error\":"+q(e.getMessage())+",\"line\":"+e.getLocation().getLine()+",\"column\":"+e.getLocation().getColumn()+"}");}
    }catch(InvocationTargetException failure){
      Throwable e=failure.getCause();System.out.println("{\"error\":"+q(e.getMessage())+",\"line\":"+get(e,"getLine")+",\"column\":"+get(e,"getColumn")+"}");
    }
  }catch(Throwable e){System.out.println("{\"error\":"+q(e.toString())+",\"line\":0,\"column\":0}");}}
}
