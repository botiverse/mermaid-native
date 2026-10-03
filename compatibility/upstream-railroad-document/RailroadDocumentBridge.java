import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Transport for real production document state, never generated Mermaid source. */
public class RailroadDocumentBridge {
 static String q(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
 static String json(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){List<String>a=new ArrayList<>();((Map<?,?>)o).forEach((k,v)->a.add(q(k.toString())+":"+json(v)));return "{"+String.join(",",a)+"}";}if(o instanceof Iterable){List<String>a=new ArrayList<>();for(Object v:(Iterable<?>)o)a.add(json(v));return "["+String.join(",",a)+"]";}throw new IllegalArgumentException(o.getClass().getName());}
 static String decode(String s){return s.equals("-")?null:new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}

 static Object rule(RailroadRule r){if(r==null)return null;return Map.of("name",r.getName(),"definition",node(r.getDefinition()));}
 static Object node(RailroadNode n){
  if(n instanceof RailroadTerminal)return Map.of("type","terminal","value",((RailroadTerminal)n).getLabel());
  throw new IllegalArgumentException("This original file's transport supports terminal expressions only");
 }
 public static void main(String[] args){
  RailroadDocument doc=new RailroadDocument();Scanner in=new Scanner(System.in,StandardCharsets.UTF_8);
  while(in.hasNextLine()){
   String[] raw=in.nextLine().split("\\t",-1);String op=raw[0];String[] a=new String[raw.length-1];for(int i=1;i<raw.length;i++)a[i-1]=decode(raw[i]);
   Object value=null;
   try{switch(op){
    case "clear":doc.clear();break;
    case "addRule":if(!a[1].equals("terminal"))throw new IllegalArgumentException("Unsupported expression");doc.addRule(new RailroadRule(a[0],new RailroadTerminal(a[2])));break;
    case "getRules":List<Object> rs=new ArrayList<>();for(RailroadRule r:doc.rules())rs.add(rule(r));value=rs;break;
    case "getRule":value=rule(doc.rule(a[0]));break;
    case "setTitle":case "setDiagramTitle":doc.setTitle(a[0]);break;
    case "getTitle":case "getDiagramTitle":value=doc.getTitle();break;
    case "setAccTitle":doc.setAccessibilityTitle(a[0]);break;
    case "getAccTitle":value=doc.getAccessibilityTitle();break;
    case "setAccDescription":doc.setAccessibilityDescription(a[0]);break;
    case "getAccDescription":value=doc.getAccessibilityDescription();break;
    default:throw new IllegalArgumentException("Unknown operation "+op);
   }System.out.println(json(Collections.singletonMap("value",value)));}
   catch(Exception e){System.out.println(json(Map.of("error",e.toString())));}
  }
 }
}
