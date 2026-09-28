import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class FlowNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }

 static String mapping(Map<String,String> values){List<String> result=new ArrayList<>();for(Map.Entry<String,String> e:values.entrySet())result.add(q(e.getKey())+":"+q(e.getValue()));return "{"+String.join(",",result)+"}";}
 static String strings(List<String> values){List<String> result=new ArrayList<>();for(String s:values)result.add(q(s));return "["+String.join(",",result)+"]";}
 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
  MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
  if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
  MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();
  if(diagram instanceof SwimlaneDiagram)diagram=((SwimlaneDiagram)diagram).getFlowchart();
  if(!(diagram instanceof FlowchartDiagram)){System.out.println("{\"error\":\"Native produced another diagram type\"}");return;}
  FlowchartDiagram d=(FlowchartDiagram)diagram;
  List<String> nodes=new ArrayList<>(),edges=new ArrayList<>(),groups=new ArrayList<>();
  for(FlowNode n:d.getNodes())nodes.add("{\"id\":"+q(n.getId())+",\"label\":"+q(n.getLabel())+",\"shape\":"+q(n.getShape().name())+",\"labelType\":"+q(n.getLabelType())+",\"borders\":"+q(n.getBorders())+",\"styles\":"+strings(n.getStyles())+",\"classes\":"+strings(n.getClasses())+",\"createdByStyle\":"+n.getCreatedByStyle()+",\"metadata\":"+mapping(n.getMetadata())+"}");
  for(FlowEdge e:d.getEdges())edges.add("{\"from\":"+q(e.getSourceId())+",\"to\":"+q(e.getTargetId())+",\"style\":"+q(e.getStyle().name())+",\"label\":"+q(e.getLabel())+",\"fromMarker\":"+q(e.getFromMarker().name())+",\"toMarker\":"+q(e.getToMarker().name())+",\"length\":"+e.getLength()+",\"id\":"+q(e.getId())+",\"labelType\":"+q(e.getLabelType())+",\"styles\":"+strings(e.getStyles())+",\"interpolate\":"+q(e.getInterpolate())+",\"animate\":"+e.getAnimate()+",\"animation\":"+q(e.getAnimation())+"}");
  for(FlowSubgraph g:d.getSubgraphs())groups.add("{\"id\":"+q(g.getId())+",\"label\":"+q(g.getLabel())+",\"nodes\":"+strings(g.getNodeIds())+",\"direction\":"+q(g.getDirection()==null?null:g.getDirection().name())+",\"labelType\":"+q(g.getLabelType())+",\"collapsed\":"+g.getCollapsed()+"}");
  List<String> definitions=new ArrayList<>();for(Map.Entry<String,List<String>> item:d.getClassDefinitions().entrySet())definitions.add(q(item.getKey())+":"+strings(item.getValue()));
  List<String> interactions=new ArrayList<>();for(FlowInteraction i:d.getInteractions())interactions.add("{\"nodeId\":"+q(i.getNodeId())+",\"value\":"+q(i.getValue())+",\"callback\":"+i.getCallback()+",\"arguments\":"+q(i.getArguments())+",\"tooltip\":"+q(i.getTooltip())+",\"target\":"+q(i.getTarget())+"}");
  System.out.println("{\"defaultInterpolate\":"+q(d.getDefaultInterpolate())+",\"definitions\":{"+String.join(",",definitions)+"},\"interactions\":["+String.join(",",interactions)+"],\"defaultEdgeStyles\":"+strings(d.getDefaultEdgeStyles())+",\"direction\":"+q(d.getDirection().name())+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"nodes\":["+String.join(",",nodes)+"],\"edges\":["+String.join(",",edges)+"],\"subgraphs\":["+String.join(",",groups)+"]}");
 }
}
