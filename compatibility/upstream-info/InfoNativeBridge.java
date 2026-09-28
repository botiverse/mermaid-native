import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class InfoNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"lexerErrors\":[],\"parserErrors\":[{\"message\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}]}");return;}
 if(!((MermaidParseResult.Success)result).getDiagram().getClass().getSimpleName().equals("InfoDiagram"))throw new IllegalStateException("Wrong Native model");
 System.out.println("{\"lexerErrors\":[],\"parserErrors\":[],\"value\":{\"$type\":\"Info\"}}");
 }
}
