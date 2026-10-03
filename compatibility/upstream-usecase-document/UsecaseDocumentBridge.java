import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Direct mutable draft operations; never generate Mermaid source or emulate publication state. */
public class UsecaseDocumentBridge {
 static String json(Object o){return UsecaseAstNativeBridge.json(o);}
 static Object unpack(UsecaseJsonValue v){
  if(v instanceof UsecaseJsonValue.ObjectValue){Map<String,Object> m=new LinkedHashMap<>();((UsecaseJsonValue.ObjectValue)v).getValue().forEach((k,x)->m.put(k,unpack(x)));return m;}
  if(v instanceof UsecaseJsonValue.ArrayValue){List<Object> a=new ArrayList<>();for(var x:((UsecaseJsonValue.ArrayValue)v).getValue())a.add(unpack(x));return a;}
  if(v instanceof UsecaseJsonValue.StringValue)return ((UsecaseJsonValue.StringValue)v).getValue();
  if(v instanceof UsecaseJsonValue.NumberValue)return ((UsecaseJsonValue.NumberValue)v).getValue();
  if(v instanceof UsecaseJsonValue.BooleanValue)return ((UsecaseJsonValue.BooleanValue)v).getValue();return null;
 }
 static Object parse(String s){return unpack(UsecaseJsonParser.INSTANCE.parseOrderedJsonObject("{\"v\":"+s+"}",1,1).getValue().get("v"));}
 static Map<String,Object> map(Object x){return (Map<String,Object>)x;}
 static String str(Map<String,Object> m,String k,String fallback){return m.get(k)==null?fallback:m.get(k).toString();}
 static boolean bool(Map<String,Object> m,String k){return Boolean.TRUE.equals(m.get(k));}
 static UsecaseAttributes attributes(Map<String,Object> m){Map<String,String> styles=new LinkedHashMap<>();for(Object x:(List<?>)m.getOrDefault("styles",List.of())){String[] pair=x.toString().split(":",2);styles.put(pair[0],pair.length>1?pair[1]:"");}return new UsecaseAttributes(Map.of("business",Boolean.toString(bool(m,"business"))),str(m,"stereotype",null),(List<String>)m.getOrDefault("classes",List.of()),styles,str(m,"parentId",null));}
 static UsecaseJsonValue jsonValue(Object x){if(x==null)return UsecaseJsonValue.NullValue.INSTANCE;if(x instanceof String)return new UsecaseJsonValue.StringValue((String)x);if(x instanceof Boolean)return new UsecaseJsonValue.BooleanValue((Boolean)x);if(x instanceof Number)return new UsecaseJsonValue.NumberValue(((Number)x).doubleValue());if(x instanceof Map){Map<String,UsecaseJsonValue> out=new LinkedHashMap<>();map(x).forEach((k,v)->out.put(k,jsonValue(v)));return new UsecaseJsonValue.ObjectValue(out);}List<UsecaseJsonValue> out=new ArrayList<>();for(Object v:(List<?>)x)out.add(jsonValue(v));return new UsecaseJsonValue.ArrayValue(out);}
 static void set(UsecaseDraft d,List<?> path,Object value){
  String field=path.get(0).toString();String key=path.size()>1?path.get(1).toString():null;
  if(key==null){switch(field){
   case "notes":if(value!=null)throw new IllegalArgumentException("Expected null notes import");d.setNotes(null);return;
   case "direction":d.setDirection(FlowDirection.valueOf(value.toString()));return;
   case "relationshipCounter":d.setRelationshipCounter(((Number)value).intValue());return;
   case "noteCounter":d.setNoteCounter(((Number)value).intValue());return;
   case "ast":d.setAst(map(value));return;
   case "accTitle":d.setAccTitle(value.toString());return;
   case "accDescription":d.setAccDescription(value.toString());return;
   default:throw new IllegalArgumentException("Unsupported draft field "+field);
  }}
  if(path.size()==3 && field.equals("actors") && path.get(2).equals("label")){UsecaseActor a=d.getActors().get(key);d.getActors().put(key,new UsecaseActor(a.getId(),value.toString(),a.getType(),a.getIcon(),a.getBusiness(),a.getLabelType()));return;}
  if(field.equals("relationships") && key.equals("length")){int n=((Number)value).intValue();while(d.getRelationships().size()>n)d.getRelationships().remove(d.getRelationships().size()-1);if(d.getRelationships().size()!=n)throw new IllegalArgumentException("Sparse relationship array");return;}
  if(field.equals("symbols")){d.getSymbols().put(key,value.toString());return;}
  Map<String,Object> m=map(value);String id=str(m,"id",key),label=str(m,"label",id);
  switch(field){
   case "actors":d.getActors().put(key,new UsecaseActor(id,label,UsecaseActorType.valueOf(str(m,"type","normal").toUpperCase(Locale.ROOT)),str(m,"icon",null),bool(m,"business"),str(m,"labelType","text")));d.getAttributes().put(id,attributes(m));break;
   case "useCases":d.getUseCases().put(key,new UsecaseNode(id,label,str(m,"shape","ellipse").equals("rect")?UsecaseShape.RECTANGLE:UsecaseShape.ELLIPSE,str(m,"labelType","text")));d.getAttributes().put(id,attributes(m));break;
   case "notes":d.getNotes().put(key,new UsecaseNote(str(m,"target",null),label,id));break;
   case "jsonNodes":Map<String,UsecaseJsonValue> values=((UsecaseJsonValue.ObjectValue)jsonValue(m.get("value"))).getValue();d.getJsonNodes().put(key,new UsecaseJsonNode(id,json(m.get("value")),new UsecaseOrderedJsonObject(values,(Map)m.get("propertyOrder"))));d.getAttributes().put(id,attributes(m));break;
   case "classDefs":d.getClassDefs().put(key,attributes(m).getStyles());break;
   case "relationships":int code=((Number)m.get("arrowType")).intValue();String start=code==1?"arrow":code==5?"circle":code==6?"cross":"none";String end=code==0?"arrow":code==3?"circle":code==4?"cross":"none";String type=str(m,"type","association");boolean dashed=type.equals("include")||type.equals("extend");if(type.equals("generalization"))end="generalization";UsecaseRelationship edge=new UsecaseRelationship(str(m,"source",null),str(m,"target",null),dashed?type:str(m,"label",null),id,start,end,dashed,bool(m,"explicitId"),((Number)m.getOrDefault("minlen",1)).intValue(),bool(m,"animate"),str(m,"animation",null),str(m,"labelType",null));int at=Integer.parseInt(key);if(at==d.getRelationships().size())d.getRelationships().add(edge);else d.getRelationships().set(at,edge);d.getAttributes().put(id,attributes(m));break;
   default:throw new IllegalArgumentException("Unsupported draft collection "+field);
  }
 }
 static Object configuration(UsecaseDocumentConfig c){return Map.of("ellipsePadding",c.getEllipsePadding(),"rectanglePadding",c.getRectanglePadding());}
 static Object draft(UsecaseDraft d,int id){Map<String,Object> out=new LinkedHashMap<>();for(String key:List.of("actors","useCases","systemBoundaries","notes","jsonNodes","classDefs","symbols"))out.put(key,Map.of("$map",List.of()));out.put("relationships",List.of());out.put("direction",d.getDirection().name());out.put("relationshipCounter",d.getRelationshipCounter());out.put("noteCounter",d.getNoteCounter());out.put("accTitle",d.getAccTitle());out.put("accDescription",d.getAccDescription());out.put("config",configuration(d.getConfig()));out.put("$draftId",id);return out;}
 static Object getter(UsecaseDocument doc,String name,String id){Map<String,Object> s=UsecaseAstNativeBridge.snapshot(doc);switch(name){
  case "getAST":return s.get("ast");case "getDirection":return s.get("direction");case "getAccTitle":return doc.getAccessibilityTitle();case "getAccDescription":return doc.getAccessibilityDescription();case "getDiagramTitle":return doc.getTitle();case "getConfig":return configuration(doc.configuration());
  case "getData":return Map.of("nodes",doc.usecaseLabelData());
  case "getRelationships":return s.getOrDefault("relationships",List.of());
 }
 String key=switch(name){case "getActor","getActors"->"actors";case "getUseCases"->"useCases";case "getSystemBoundaries"->"boundaries";case "getNotes"->"notes";case "getJsonNodes"->"jsonNodes";case "getClassDefs"->"classDefs";default->throw new IllegalArgumentException(name);};
 List<Object> entries=new ArrayList<>();Object values=s.getOrDefault(key,List.of());if(values instanceof Map){map(values).forEach((k,v)->entries.add(List.of(k,v)));}else for(Object row:(List<?>)values){if(name.equals("getActor")&&id.equals(map(row).get("id")))return row;entries.add(List.of(map(row).get("id"),row));}return name.equals("getActor")?null:Map.of("$map",entries);
 }
 public static void main(String[] args){UsecaseDocument doc=new UsecaseDocument();Map<Integer,UsecaseDraft> drafts=new HashMap<>();Scanner in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){Map<String,Object> c=map(parse(in.nextLine()));String op=c.get("op").toString();List<?> a=(List<?>)c.get("args");Object result=null;try{switch(op){
  case "clear":doc.clear();break;
  case "create":int id=((Number)a.get(0)).intValue();UsecaseDraft d=doc.createModel();drafts.put(id,d);result=draft(d,id);break;
  case "set":set(drafts.get(((Number)a.get(0)).intValue()),(List<?>)a.get(1),a.get(2));break;
  case "empty":UsecaseDraft target=drafts.get(((Number)a.get(0)).intValue());if(a.get(1).equals("notes"))target.getNotes().clear();else throw new IllegalArgumentException("Unknown clear collection");break;
  case "commit":doc.commit(drafts.get(((Number)a.get(0)).intValue()));break;
  case "incomplete":UsecaseDraft bad=doc.createModel();bad.setNotes(null);doc.commit(bad);break;
  case "setDiagramTitle":doc.setTitle(a.get(0).toString());break;
  case "setAccTitle":doc.setAccessibilityTitle(a.get(0).toString());break;
  case "setAccDescription":doc.setAccessibilityDescription(a.get(0).toString());break;
  default:result=getter(doc,op,a.isEmpty()?null:a.get(0).toString());
 }System.out.println(json(Collections.singletonMap("value",result)));}catch(Exception e){System.out.println(json(Map.of("error",e.getMessage())));}}}
}
