import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class TreemapNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void flatten(TreemapNode n,List<String> rows){rows.add("{\"$type\":\"TreemapRow\",\"item\":{\"$type\":"+q(n.getValue()==null?"Section":"Leaf")+",\"name\":"+q(n.getLabel())+(n.getClassSelector()==null?"":",\"classSelector\":"+q(n.getClassSelector()))+(n.getValue()==null?"":",\"value\":"+n.getValue())+"}}");for(var child:n.getChildren())flatten(child,rows);}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"lexerErrors\":[],\"parserErrors\":[{\"message\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}]}");return;}
 TreemapDiagram d=(TreemapDiagram)((MermaidParseResult.Success)result).getDiagram();List<String> rows=new ArrayList<>();for(var e:d.getClasses().entrySet())rows.add("{\"$type\":\"ClassDefStatement\",\"className\":"+q(e.getKey())+",\"styleText\":"+q(e.getValue())+"}");for(var n:d.getRoots())flatten(n,rows);
 System.out.println("{\"lexerErrors\":[],\"parserErrors\":[],\"value\":{\"$type\":\"Treemap\",\"TreemapRows\":["+String.join(",",rows)+"]}}");
 }
}
