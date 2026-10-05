import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Converts transport values only; identities, membership, styling and graph projection are Native. */
public class EntityDocumentBridge {
 static Map<String,Object> map(Object x){return (Map<String,Object>)x;}
 static Map<String,Object> node(EntityDocumentNode n){
  if(n==null)return null;
  Map<String,Object> m=new LinkedHashMap<>();m.put("id",n.getId());m.put("label",n.getLabel());
  m.put("parentId",n.getParentId());m.put("isGroup",n.isGroup());m.put("cssStyles",n.getStyles());m.put("classes",n.getClasses());return m;
 }
 static Map<String,Object> edge(EntityDocumentEdge e){return Map.of("start",e.getStart(),"end",e.getEnd(),"label",e.getLabel());}
 public static void main(String[] args){
  EntityRelationshipDocument doc=new EntityRelationshipDocument();Scanner input=new Scanner(System.in,StandardCharsets.UTF_8);
  while(input.hasNextLine()){
   String[] fields=input.nextLine().split("\\t",-1);String op=fields[0];List<Object> a=new ArrayList<>();
   for(int i=1;i<fields.length;i++)a.add(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(fields[i]),StandardCharsets.UTF_8)));
   Object value=null;
   try{switch(op){
    case "clear":doc.clear();break;
    case "addEntity":value=node(doc.addEntity((String)a.get(0)));break;
    case "getEntity":value=node(doc.entity((String)a.get(0)));break;
    case "addSubGraph":value=doc.addSubgraph((String)map(a.get(0)).get("text"),(List<String>)(Object)a.get(1),(String)map(a.get(2)).get("text"));break;
    case "getSubGraphs":{
     List<Object> groups=new ArrayList<>();for(EntitySubgraph g:doc.subgraphs())groups.add(Map.of("id",g.getId(),"title",g.getTitle(),"nodes",g.getNodeIds(),"classes",g.getClasses(),"cssStyles",g.getStyles()));value=groups;break;
    }
    case "addRelationship":{
     Map<String,Object> spec=map(a.get(3));doc.addRelationship((String)a.get(0),(String)a.get(1),(String)a.get(2),EntityCardinality.valueOf((String)spec.get("cardA")),EntityCardinality.valueOf((String)spec.get("cardB")),spec.get("relType").equals("IDENTIFYING"));break;
    }
    case "getRelationships":{
     List<Object> rels=new ArrayList<>();for(EntityDocumentEdge e:doc.data().getEdges())rels.add(Map.of("entityA",e.getStart(),"entityB",e.getEnd(),"roleA",e.getLabel()));value=rels;break;
    }
    case "setClass":doc.addClasses((List<String>)(Object)a.get(0),(List<String>)(Object)a.get(1));break;
    case "addCssStyles":doc.addStyles((List<String>)(Object)a.get(0),(List<String>)(Object)a.get(1));break;
    case "getData":{
     EntityDocumentData data=doc.data();List<Object> nodes=new ArrayList<>(),edges=new ArrayList<>();for(var n:data.getNodes())nodes.add(node(n));for(var e:data.getEdges())edges.add(edge(e));value=Map.of("nodes",nodes,"edges",edges);break;
    }
    default:throw new IllegalArgumentException("Unsupported ER operation "+op);
   }System.out.println(TreeDocumentsBridge.json(Collections.singletonMap("value",value)));}
   catch(Exception e){System.out.println(TreeDocumentsBridge.json(Map.of("error",e.toString())));}
  }
 }
}
