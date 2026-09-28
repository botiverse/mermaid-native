import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class QuadrantNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=new QuadrantParser(source).parse();
 if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}");return;}
 QuadrantChartDiagram d=(QuadrantChartDiagram)((MermaidParseResult.Success)result).getDiagram();List<String> labels=new ArrayList<>(),points=new ArrayList<>(),classes=new ArrayList<>();
 if(d.getXAxis().getLowDefined())labels.add(q("setXAxisLeftText")+":"+label(d.getXAxis().getLowLabel(),d.getXAxis().getLowType()));
 if(d.getXAxis().getHighDefined())labels.add(q("setXAxisRightText")+":"+label(d.getXAxis().getHighLabel(),d.getXAxis().getHighType()));
 if(d.getYAxis().getLowDefined())labels.add(q("setYAxisBottomText")+":"+label(d.getYAxis().getLowLabel(),d.getYAxis().getLowType()));
 if(d.getYAxis().getHighDefined())labels.add(q("setYAxisTopText")+":"+label(d.getYAxis().getHighLabel(),d.getYAxis().getHighType()));
 for(int i=0;i<d.getQuadrantLabels().size();i++)if(d.getQuadrantLabels().get(i)!=null)labels.add(q("setQuadrant"+(i+1)+"Text")+":"+label(d.getQuadrantLabels().get(i),d.getQuadrantLabelTypes().get(i)));
 for(QuadrantPoint p:d.getPoints())points.add("{\"label\":"+label(p.getLabel(),p.getLabelType())+",\"className\":"+q(p.getClassName())+",\"styles\":"+strings(p.getStyles())+",\"x\":"+q(p.getSourceX())+",\"y\":"+q(p.getSourceY())+"}");
 for(var entry:d.getClasses().entrySet())classes.add(q(entry.getKey())+":"+strings(entry.getValue()));
 System.out.println("{\"title\":"+q(d.getTitle())+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"labels\":{"+String.join(",",labels)+"},\"points\":["+String.join(",",points)+"],\"classes\":{"+String.join(",",classes)+"}}");
 }
 static String label(String text,String type){return "{\"text\":"+q(text)+",\"type\":"+q(type)+"}";}
 static String strings(List<String> values){return "["+String.join(",",values.stream().map(QuadrantNativeBridge::q).toList())+"]";}
}
