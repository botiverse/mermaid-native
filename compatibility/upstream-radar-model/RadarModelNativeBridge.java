import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
import build.raft.mermaid.layout.simple.RadarGeometry;
public class RadarModelNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }



 public static void main(String[] args) {
  Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);
  while(lines.hasNextLine()) {
   String source=new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8);
   if(source.startsWith("R|")) {
    String[] v=source.split("\\|");
    System.out.println(RadarGeometry.INSTANCE.relativeRadius(Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3]),Double.parseDouble(v[4])));
   } else render(source);
  }
 }
 static void render(String source) {
  MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
  if(result instanceof MermaidParseResult.Failure) {
   System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}");return;
  }
  RadarChartDiagram d=(RadarChartDiagram)((MermaidParseResult.Success)result).getDiagram();
  List<String> axes=new ArrayList<>(),curves=new ArrayList<>();
  for(RadarAxis a:d.getAxes())axes.add("{\"name\":"+q(a.getId())+",\"label\":"+q(a.getLabel())+"}");
  for(RadarCurve c:d.getCurves())curves.add("{\"name\":"+q(c.getId())+",\"label\":"+q(c.getLabel())+",\"entries\":"+d.axisValues(c)+"}");
  RadarResolvedOptions o=d.getResolvedOptions();
  System.out.println("{\"getDiagramTitle\":"+q(d.getTitle()==null?"":d.getTitle())+",\"getAccTitle\":"+q(d.getAccessibilityTitle()==null?"":d.getAccessibilityTitle())+",\"getAccDescription\":"+q(d.getAccessibilityDescription()==null?"":d.getAccessibilityDescription())+",\"getAxes\":["+String.join(",",axes)+"],\"getCurves\":["+String.join(",",curves)+"],\"getOptions\":{\"ticks\":"+o.getTicks()+",\"max\":"+o.getMaximum()+",\"min\":"+o.getMinimum()+",\"showLegend\":"+o.getShowLegend()+",\"graticule\":"+q(o.getGraticule())+"}}");
 }
}
