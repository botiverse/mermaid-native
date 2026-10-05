import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.layout.*;
import build.raft.mermaid.layout.simple.*;

/** Original geometry and threshold arguments go directly to production Native APIs. */
public class LayoutQualityBridge {
 static Map<String,Object> map(Object o){return UsecaseDocumentBridge.map(o);}
 static ScenePoint point(Object o){var p=map(o);return new ScenePoint(((Number)p.get("x")).doubleValue(),((Number)p.get("y")).doubleValue());}
 static Double number(Object o){return o==null?null:((Number)o).doubleValue();}
 static Object safe(Object o){
  if(o instanceof Double&&!Double.isFinite((Double)o))return Map.of("$number",Double.isNaN((Double)o)?"NaN":((Double)o)>0?"Infinity":"-Infinity");
  if(o instanceof Map){var m=new LinkedHashMap<String,Object>();map(o).forEach((k,v)->m.put(k,safe(v)));return m;}
  if(o instanceof Iterable){var l=new ArrayList<>();for(Object v:(Iterable<?>)o)l.add(safe(v));return l;}return o;
 }
 static Object result(LayoutQualityResult r){var thresholds=r.getThresholdResults();Map<String,Object> out=null;
  if(thresholds!=null){out=new LinkedHashMap<>();for(var e:thresholds.entrySet()){var v=e.getValue();out.put(e.getKey(),Map.of("value",v.getValue(),"threshold",v.getThreshold(),"pass",v.getPass()));}}
  var m=new LinkedHashMap<String,Object>();m.put("scores",r.getScores().values());m.put("thresholdResults",out);return m;
 }
 public static void main(String[]args){var in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){
  var row=map(UsecaseDocumentBridge.parse(new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8)));var argv=(List<?>)row.get("args");Object value;
  if(row.get("op").equals("cross")){var a=map(argv.get(0));var b=map(argv.get(1));value=LayoutQualityScorer.INSTANCE.segmentsCross(point(a.get("a")),point(a.get("b")),point(b.get("a")),point(b.get("b")));}
  else {var input=LayoutValidationBridge.input(map(argv.get(0)));Map<String,LayoutQualityThreshold> thresholds=null;
   if(argv.size()>1 && argv.get(1)!=null){thresholds=new LinkedHashMap<>();for(var e:map(argv.get(1)).entrySet()){if(e.getValue()==null)continue;var v=map(e.getValue());thresholds.put(e.getKey(),new LayoutQualityThreshold(number(v.get("min")),number(v.get("max"))));}}
   value=result(LayoutQualityScorer.INSTANCE.score(input,thresholds));
  }System.out.println(TreeDocumentsBridge.json(safe(value)));
 }}
}
