import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class TreeViewNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"lexerErrors\":[],\"parserErrors\":[{\"message\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}]}");return;}
 TreeViewDiagram d=(TreeViewDiagram)((MermaidParseResult.Success)result).getDiagram();List<String> nodes=new ArrayList<>();
 for(var n:d.getNodes())nodes.add("{\"name\":"+q(n.getLabel()+(n.getDirectory()&&!n.getLabel().equals("/")?"/":""))+(n.getSourceIndent()==null?"":",\"indent\":"+n.getSourceIndent())+(n.getClassAnnotation()==null?"":",\"classAnnotation\":"+q(n.getClassAnnotation()))+(n.getIconAnnotation()==null?"":",\"iconAnnotation\":"+q(n.getIconAnnotation()))+(n.getDescription()==null?"":",\"descAnnotation\":"+q(n.getDescription()))+"}");
 System.out.println("{\"lexerErrors\":[],\"parserErrors\":[],\"value\":{\"$type\":\"TreeView\",\"nodes\":["+String.join(",",nodes)+"],\"title\":"+q(d.getTitle())+",\"accTitle\":"+q(d.getAccTitle())+",\"accDescr\":"+q(d.getAccDescription())+"}}");
 }
}
