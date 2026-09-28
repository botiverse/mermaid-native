import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class XyNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().toString())+"}");return;}
 MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();if(!(diagram instanceof XyChartDiagram)){System.out.println("{\"error\":\"Another diagram type\"}");return;}XyChartDiagram d=(XyChartDiagram)diagram;List<String> series=new ArrayList<>(),cats=new ArrayList<>();
 for(int i=0;i<d.getXAxis().getCategories().size();i++)cats.add("{\"text\":"+q(d.getXAxis().getCategories().get(i))+",\"type\":"+q(i<d.getXAxis().getCategoryTypes().size()?d.getXAxis().getCategoryTypes().get(i):"text")+"}");
 for(XySeries item:d.getSeries()){List<String> points=new ArrayList<>();for(int i=0;i<item.getValues().size();i++)points.add("{\"value\":"+item.getValues().get(i)+",\"label\":"+q(i<item.getLabels().size()?item.getLabels().get(i):"")+"}");series.add("{\"kind\":"+q(item.getKind().name())+",\"title\":"+q(item.getTitle())+",\"titleType\":"+q(item.getTitleType())+",\"points\":["+String.join(",",points)+"]}");}
 NumericAxis x=d.getXAxis().getRange();
 System.out.println("{\"title\":"+q(d.getTitle())+",\"orientation\":"+q(d.getOrientation()==null?null:d.getOrientation().name().toLowerCase())+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"xTitle\":"+q(d.getXAxis().getTitle())+",\"xTitleType\":"+q(d.getXAxis().getTitleType())+",\"yTitle\":"+q(d.getYAxis().getTitle())+",\"yTitleType\":"+q(d.getYAxis().getTitleType())+",\"xRange\":"+(x==null?"null":"["+x.getMinimum()+","+x.getMaximum()+"]")+",\"yExplicit\":"+d.getYAxis().getExplicitRange()+",\"yMin\":"+d.getYAxis().getMinimum()+",\"yMax\":"+d.getYAxis().getMaximum()+",\"categories\":["+String.join(",",cats)+"],\"series\":["+String.join(",",series)+"]}");
 }
}
