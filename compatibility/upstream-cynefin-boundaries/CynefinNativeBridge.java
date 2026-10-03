import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
import build.raft.mermaid.layout.*;

/** Direct production document/geometry transport. */
public class CynefinNativeBridge {
 static String q(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
 static String json(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){List<String>a=new ArrayList<>();((Map<?,?>)o).forEach((k,v)->a.add(q(k.toString())+":"+json(v)));return "{"+String.join(",",a)+"}";}if(o instanceof Iterable){List<String>a=new ArrayList<>();for(Object v:(Iterable<?>)o)a.add(json(v));return "["+String.join(",",a)+"]";}throw new IllegalArgumentException(o.getClass().getName());}
 static String decode(String s){return s.equals("-")?null:new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}

 static CynefinDomain domain(String value){return CynefinDomain.valueOf(value.toUpperCase(Locale.ROOT));}
 static double number(String value){return Double.parseDouble(value);}
 static Double optional(String[] a,int index){return index>=a.length||a[index]==null?null:number(a[index]);}
 static Object domains(CynefinDocument doc){Map<String,Object> result=new LinkedHashMap<>();for(CynefinDomainBlock b:doc.domains()){List<Object> items=new ArrayList<>();for(String text:b.getItems())items.add(Map.of("label",text));String name=b.getDomain().name().toLowerCase(Locale.ROOT);result.put(name,Map.of("name",name,"items",items));}return result;}
 static Object transitions(CynefinDocument doc){List<Object> result=new ArrayList<>();for(CynefinTransition t:doc.transitions()){Map<String,Object> row=new LinkedHashMap<>();row.put("from",t.getFrom().name().toLowerCase(Locale.ROOT));row.put("to",t.getTo().name().toLowerCase(Locale.ROOT));if(t.getLabel()!=null)row.put("label",t.getLabel());result.add(row);}return result;}
 public static void main(String[] args){
  CynefinDocument doc=new CynefinDocument();CynefinBoundaries geometry=CynefinBoundaries.INSTANCE;Scanner in=new Scanner(System.in,StandardCharsets.UTF_8);
  while(in.hasNextLine()){
   String[] raw=in.nextLine().split("\\t",-1);String op=raw[0];String[] a=new String[raw.length-1];for(int i=1;i<raw.length;i++)a[i-1]=decode(raw[i]);Object value=null;
   try{switch(op){
    case "clear":doc.clear();break;
    case "setDomains":{if(a[0]==null){doc.setDomains(null);break;}int count=Integer.parseInt(a[0]),at=1;List<CynefinDomainBlock> blocks=new ArrayList<>();for(int i=0;i<count;i++){CynefinDomain name=domain(a[at++]);int n=Integer.parseInt(a[at++]);List<String> labels=new ArrayList<>();for(int j=0;j<n;j++)labels.add(a[at++]);blocks.add(new CynefinDomainBlock(name,labels));}doc.setDomains(blocks);break;}
    case "setTransitions":{if(a[0]==null){doc.setTransitions(null);break;}int count=Integer.parseInt(a[0]),at=1;List<CynefinTransition> links=new ArrayList<>();for(int i=0;i<count;i++){links.add(new CynefinTransition(domain(a[at]),domain(a[at+1]),a[at+2]));at+=3;}doc.setTransitions(links);break;}
    case "getDomains":value=domains(doc);break;
    case "getTransitions":value=transitions(doc);break;
    case "getConfig":value=doc.configuration();break;
    case "seededRandom":value=geometry.seededRandom(number(a[0]));break;
    case "hashString":value=geometry.hashString(a[0]);break;
    case "resolveSeed":value=geometry.resolveSeed(optional(a,0),a[1]);break;
    case "generateFoldPath":value=geometry.fold(number(a[0]),number(a[1]),number(a[2]),optional(a,3)).svgData();break;
    case "generateHorizontalBoundary":value=geometry.horizontal(number(a[0]),number(a[1]),number(a[2]),optional(a,3)).svgData();break;
    case "generateCliffPath":value=geometry.cliff(number(a[0]),number(a[1])).svgData();break;
    case "generateConfusionPath":value=geometry.confusion(number(a[0]),number(a[1]),number(a[2]),number(a[3])).svgData();break;
    default:throw new IllegalArgumentException("Unknown operation "+op);
   }System.out.println(json(Collections.singletonMap("value",value)));}catch(Exception e){System.out.println(json(Map.of("error",e.toString())));}
  }
 }
}
