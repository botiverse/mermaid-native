import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class RailroadNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}

 public static void main(String[] args){Scanner s=new Scanner(System.in,StandardCharsets.UTF_8);while(s.hasNextLine()){String input=s.nextLine();try{var parsed=MermaidParser.INSTANCE.parse(decode(input));
 if(parsed instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)parsed).getDiagnostics().get(0).getMessage())+"}");continue;}
 var d=(RailroadDiagram)((MermaidParseResult.Success)parsed).getDiagram();List<String> rules=new ArrayList<>();for(var r:d.getRules())rules.add("{\"name\":"+q(r.getName())+",\"definition\":"+node(r.getDefinition())+"}");
 System.out.println("{\"title\":"+q(d.getTitle())+",\"accTitle\":"+q(d.getAccTitle())+",\"accDescr\":"+q(d.getAccDescription())+",\"rules\":["+String.join(",",rules)+"]}");
 }catch(Throwable e){System.out.println("{\"error\":"+q(e.getMessage()==null?e.getClass().getName():e.getMessage())+"}");}}}
 static String node(RailroadNode n){String type,field,value;
 if(n.getClass().getSimpleName().equals("RailroadRepetition")){try{var cls=n.getClass();Object max=cls.getMethod("getMax").invoke(n);return "{\"type\":\"repetition\",\"element\":"+node((RailroadNode)cls.getMethod("getChild").invoke(n))+",\"min\":"+cls.getMethod("getMin").invoke(n)+",\"max\":"+max+"}";}catch(Exception e){throw new RuntimeException(e);}}

 if(n instanceof RailroadTerminal){type="terminal";field="value";value=q(((RailroadTerminal)n).getLabel());}
 else if(n instanceof RailroadNonTerminal){type="nonterminal";field="name";value=q(((RailroadNonTerminal)n).getLabel());}
 else if(n instanceof RailroadSpecial){type="special";field="text";value=q(((RailroadSpecial)n).getText());}
 else if(n instanceof RailroadSequence||n instanceof RailroadChoice){type=n instanceof RailroadSequence?"sequence":"choice";field=type.equals("sequence")?"elements":"alternatives";var children=n instanceof RailroadSequence?((RailroadSequence)n).getChildren():((RailroadChoice)n).getChildren();List<String>a=new ArrayList<>();for(var c:children)a.add(node(c));value="["+String.join(",",a)+"]";}
 else {type=n instanceof RailroadOptional?"optional":"repetition";field="element";var child=n instanceof RailroadOptional?((RailroadOptional)n).getChild():n instanceof RailroadOneOrMore?((RailroadOneOrMore)n).getChild():((RailroadZeroOrMore)n).getChild();value=node(child);}
 return "{\"type\":"+q(type)+",\""+field+"\":"+value+(type.equals("repetition")?",\"min\":"+(n instanceof RailroadOneOrMore?1:0):"")+"}";
 }
}
