import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

public class UsecaseJsonRowsNativeBridge {
  static String q(String s) {StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray())switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.append('"').toString();}
  public static void main(String[] args){Scanner in=new Scanner(System.in,StandardCharsets.UTF_8);while(in.hasNextLine()){
    String s=new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8);
    var envelope=UsecaseJsonParser.INSTANCE.parseOrderedJsonObject(s,1,1).getValue();
    var value=((UsecaseJsonValue.ObjectValue)envelope.get("value")).getValue();
    var rawOrder=((UsecaseJsonValue.ObjectValue)envelope.get("propertyOrder")).getValue();
    Map<String,List<String>> order=new LinkedHashMap<>();
    for(var e:rawOrder.entrySet()){List<String> keys=new ArrayList<>();for(var key:((UsecaseJsonValue.ArrayValue)e.getValue()).getValue())keys.add(((UsecaseJsonValue.StringValue)key).getValue());order.put(e.getKey(),keys);}
    var rows=UsecaseJsonTable.INSTANCE.rows(new UsecaseOrderedJsonObject(value,order));List<String> out=new ArrayList<>();
    for(var row:rows)out.add("{\"key\":"+q(row.getKey())+",\"accessibleKey\":"+q(row.getAccessibleKey())+",\"value\":"+q(row.getValue())+"}");
    System.out.println("["+String.join(",",out)+"]");
  }}
}
