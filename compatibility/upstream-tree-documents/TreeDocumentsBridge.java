import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Operations call production document APIs directly; no synthetic Mermaid source. */
public class TreeDocumentsBridge {
 static String q(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
 static String json(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){List<String>a=new ArrayList<>();((Map<?,?>)o).forEach((k,v)->a.add(q(k.toString())+":"+json(v)));return "{"+String.join(",",a)+"}";}if(o instanceof Iterable){List<String>a=new ArrayList<>();for(Object v:(Iterable<?>)o)a.add(json(v));return "["+String.join(",",a)+"]";}throw new IllegalArgumentException(o.getClass().getName());}
 static String decode(String s){return s.equals("-")?null:new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
 public static void main(String[] args){
  MindmapDocument mind=new MindmapDocument(10,200);TreeViewDocument tree=new TreeViewDocument();
  Scanner in=new Scanner(System.in,StandardCharsets.UTF_8);
  while(in.hasNextLine()){
   String[] raw=in.nextLine().split("\\t",-1);String op=raw[0];String[] a=new String[raw.length-1];for(int i=1;i<raw.length;i++)a[i-1]=decode(raw[i]);
   Object result=null;
   try{switch(op){
    case "M.clear":mind.clear();break;
    case "M.add":mind.addNode(Integer.parseInt(a[0]),a[1],a[2],Integer.parseInt(a[3]));break;
    case "M.root":result=mind.rootSnapshot();break;
    case "M.data":result=mind.layoutData();break;
    case "M.set":{
     MindmapDocumentNode n=mind.node(Integer.parseInt(a[0]));switch(a[1]){
      case "width":n.setWidth(Double.parseDouble(a[2]));break;case "height":n.setHeight(a[2]==null?null:Double.valueOf(a[2]));break;
      case "padding":n.setPadding(Double.parseDouble(a[2]));break;case "section":n.setSection(a[2]==null?null:Integer.valueOf(a[2]));break;
      case "class":n.setCssClass(a[2]);break;case "icon":n.setIcon(a[2]);break;case "x":n.setX(a[2]==null?null:Double.valueOf(a[2]));break;case "y":n.setY(a[2]==null?null:Double.valueOf(a[2]));break;
      default:throw new IllegalArgumentException("Unsupported node property "+a[1]);
     }break;
    }
    case "T.clear":tree.clear();break;
    case "T.add":tree.addNode(Integer.parseInt(a[0]),a[1],a[2].equals("directory"),a[3],a[4],a[5]);break;
    case "T.root":result=tree.rootSnapshot();break;
    case "T.count":result=tree.getCount();break;
    case "T.config":result=tree.configuration();break;
    case "T.title.set":tree.setTitle(a[0]);break;case "T.title.get":result=tree.getTitle();break;
    case "T.accTitle.set":tree.setAccessibilityTitle(a[0]);break;case "T.accTitle.get":result=tree.getAccessibilityTitle();break;
    case "T.accDescr.set":tree.setAccessibilityDescription(a[0]);break;case "T.accDescr.get":result=tree.getAccessibilityDescription();break;
    default:throw new IllegalArgumentException("Unknown operation "+op);
   }System.out.println(json(Collections.singletonMap("value",result)));}
   catch(Exception e){System.out.println(json(Map.of("error",e.getMessage())));}
  }
 }
}
