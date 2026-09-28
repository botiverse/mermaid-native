import build.raft.mermaid.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class EventModelingNativeBridge {
 public static void main(String[] args){Scanner s=new Scanner(System.in);while(s.hasNextLine()){
  String source=new String(Base64.getDecoder().decode(s.nextLine()),StandardCharsets.UTF_8);
  MermaidParseResult r=MermaidParser.INSTANCE.parse(source);
  if(r instanceof MermaidParseResult.Success && ((MermaidParseResult.Success)r).getDiagram() instanceof EventModelingDiagram) System.out.println("{\"accepted\":true}");
  else System.out.println("{\"error\":\"Native Event Modeling rejected input\"}");
 }}
}
