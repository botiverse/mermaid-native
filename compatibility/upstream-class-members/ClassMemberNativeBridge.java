import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class ClassMemberNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}

 public static void main(String[] args){Scanner scan=new Scanner(System.in,StandardCharsets.UTF_8);while(scan.hasNextLine()){String[] parts=scan.nextLine().split("\\t",2);try{
 String input=decode(parts[0]);boolean method=parts[1].equals("method");String display,css="";
 try{Class<?> cls=Class.forName("build.raft.mermaid.core.ClassMemberFormatter");Object result=cls.getMethod("format",String.class,boolean.class).invoke(cls.getField("INSTANCE").get(null),input,method);Class<?> rt=result.getClass();display=(String)rt.getMethod("getText").invoke(result);if((boolean)rt.getMethod("getItalic").invoke(result))css="font-style:italic;";else if((boolean)rt.getMethod("getUnderline").invoke(result))css="text-decoration:underline;";}
 catch(ClassNotFoundException e){boolean visible=!input.isEmpty()&&"+#~-".indexOf(input.charAt(0))>=0;ClassVisibility v=!visible?ClassVisibility.PUBLIC:input.charAt(0)=='-'?ClassVisibility.PRIVATE:input.charAt(0)=='#'?ClassVisibility.PROTECTED:input.charAt(0)=='~'?ClassVisibility.PACKAGE:ClassVisibility.PUBLIC;ClassMember member=new ClassMember(visible?input.substring(1):input,v,visible);Class<?> cls=Class.forName("build.raft.mermaid.layout.simple.SimpleMermaidLayout");var m=cls.getDeclaredMethod("classMemberLabel",ClassMember.class);m.setAccessible(true);display=(String)m.invoke(cls.getField("INSTANCE").get(null),member);}
 System.out.println("{\"displayText\":"+q(display)+",\"cssStyle\":"+q(css)+"}");
 }catch(Throwable e){System.out.println("{\"error\":"+q(e.toString())+"}");}}}
}
