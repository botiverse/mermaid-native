import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class TreeValuesNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String dec(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
 public static void main(String[] args){Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);while(input.hasNextLine()){try{
 String[] parts=input.nextLine().split("\t",-1);String rule=dec(parts[0]),text=dec(parts[1]);Object value;
 try{Class<?> c=Class.forName("build.raft.mermaid.core.TreeViewValueConverter");value=c.getMethod("convert",String.class,String.class).invoke(c.getField("INSTANCE").get(null),rule,text);}
 catch(ClassNotFoundException missing){
 if(!Set.of("INDENTATION","QUOTED_NAME","BARE_NAME","CLASS_ANNOTATION","ICON_ANNOTATION","DESC_ANNOTATION").contains(rule)){System.out.println("{\"value\":null,\"baselineAdapterOnly\":true}");continue;}
 String line=rule.equals("INDENTATION")?text+"probe":rule.endsWith("_ANNOTATION")?"probe"+text:text;
 var result=MermaidParser.INSTANCE.parse("treeView-beta\n"+line);if(!(result instanceof MermaidParseResult.Success))throw new IllegalArgumentException("Baseline production parse rejected token");
 var n=((TreeViewDiagram)((MermaidParseResult.Success)result).getDiagram()).getNodes().get(0);
 switch(rule){case "INDENTATION":value=n.getSourceIndent()==null?0:n.getSourceIndent();break;case "QUOTED_NAME":case "BARE_NAME":value=n.getLabel()+(n.getDirectory()&&!n.getLabel().equals("/")?"/":"");break;case "CLASS_ANNOTATION":value=n.getClassAnnotation();break;case "ICON_ANNOTATION":value=n.getIconAnnotation();break;case "DESC_ANNOTATION":value=n.getDescription();break;default:throw new IllegalArgumentException("Unknown baseline rule");}
 }
 System.out.println("{\"value\":"+(value instanceof Number?value:q((String)value))+"}");
 }catch(Throwable e){System.out.println("{\"error\":"+q(e.toString())+"}");}}}
}
