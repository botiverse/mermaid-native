import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class TreeViewBoxNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
 public static void main(String[] args){Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){
  try {String[] parts=input.nextLine().split("\t",-1);String method=parts[0],source=decode(parts[1]),result;
   if(method.equals("detect"))result=String.valueOf(TreeViewBoxDrawing.INSTANCE.isBoxDrawingFormat(Arrays.asList(source.split("\n",-1))));
   else if(method.equals("remap")){Map<Integer,Integer> map=new LinkedHashMap<>();if(parts.length>2&&!parts[2].isEmpty())for(String pair:parts[2].split(",")){String[] n=pair.split(":");map.put(Integer.parseInt(n[0]),Integer.parseInt(n[1]));}result=q(TreeViewBoxDrawing.INSTANCE.remapErrorLines(source,map));}
   else if(method.equals("preprocess")){var value=TreeViewBoxDrawing.INSTANCE.preprocess(source);List<String> entries=new ArrayList<>();for(var entry:value.getLineMap().entrySet())entries.add("["+entry.getKey()+","+entry.getValue()+"]");result="{\"text\":"+q(value.getText())+",\"lineMap\":["+String.join(",",entries)+"]}";}
   else result=product(source);
   System.out.println("{\"value\":"+result+"}");
  }catch(Throwable error){System.out.println("{\"error\":"+q(error.getMessage())+"}");}
 }}
 static String product(String source){var parsed=MermaidParser.INSTANCE.parse(source);if(parsed instanceof MermaidParseResult.Failure)throw new IllegalArgumentException(((MermaidParseResult.Failure)parsed).getDiagnostics().get(0).getMessage());
  TreeViewDiagram diagram=(TreeViewDiagram)((MermaidParseResult.Success)parsed).getDiagram();return node(diagram,-1);
 }
 static String node(TreeViewDiagram d,int i){List<String> children=new ArrayList<>();for(int j=0;j<d.getNodes().size();j++){Integer parent=d.getNodes().get(j).getParentIndex();if((parent==null?-1:parent)==i)children.add(node(d,j));}
  if(i<0)return "{\"id\":0,\"name\":\"/\",\"nodeType\":\"directory\",\"level\":-1,\"children\":["+String.join(",",children)+"]}";
  var n=d.getNodes().get(i);String icon=n.getIconAnnotation();return "{\"id\":"+(i+1)+",\"name\":"+q(n.getLabel())+",\"nodeType\":"+q(n.getDirectory()?"directory":"file")+",\"level\":"+(n.getSourceIndent()==null?0:n.getSourceIndent())+(n.getClassAnnotation()==null?"":",\"cssClass\":"+q(n.getClassAnnotation()))+(icon==null?"":",\"icon\":"+q(icon.isEmpty()?"none":icon))+(n.getDescription()==null||n.getDescription().isEmpty()?"":",\"description\":"+q(n.getDescription()))+",\"children\":["+String.join(",",children)+"]}";
 }
}
