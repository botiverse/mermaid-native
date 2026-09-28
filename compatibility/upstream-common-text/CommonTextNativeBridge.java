import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class CommonTextNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String dec(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
 static String json(Object o){if(o==null)return "null";if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Collection<?>){List<String> a=new ArrayList<>();for(Object v:(Collection<?>)o)a.add(json(v));return "["+String.join(",",a)+"]";}return q(o.toString());}
 public static void main(String[] args){Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){try{
 String[] parts=input.nextLine().split("\t",-1);String name=dec(parts[0]),text=dec(parts[1]);Object value;
 try{Class<?> c=Class.forName("build.raft.mermaid.core.MermaidText");value=name.equals("countOccurrence")?c.getMethod(name,String.class,String.class).invoke(c.getField("INSTANCE").get(null),text,dec(parts[2])):c.getMethod(name,String.class).invoke(c.getField("INSTANCE").get(null),text);}
 catch(ClassNotFoundException missing){
 if(name.equals("parseGenericTypes")){var method=ClassMemberFormatter.class.getDeclaredMethod("generics",String.class);method.setAccessible(true);value=method.invoke(ClassMemberFormatter.INSTANCE,text);}
 else if(name.equals("splitBreaks")||name.equals("hasBreaks")){Class<?> c=Class.forName("build.raft.mermaid.layout.simple.ClassPlacementKt");var lines=(List<?>)c.getMethod("classNoteLines",String.class).invoke(null,text);value=name.equals("splitBreaks")?lines:lines.size()>1;}
 else throw new IllegalStateException("No separately callable occurrence counter in baseline production");
 }
 System.out.println("{\"value\":"+json(value)+"}");
 }catch(Throwable e){System.out.println("{\"error\":"+q(e.toString())+"}");}}}
}
