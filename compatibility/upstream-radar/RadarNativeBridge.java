import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class RadarNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);
 if(result instanceof MermaidParseResult.Failure){MermaidDiagnostic d=((MermaidParseResult.Failure)result).getDiagnostics().get(0);System.out.println("{\"lexerErrors\":[],\"parserErrors\":[{\"message\":"+q(d.getMessage())+",\"token\":{\"startLine\":"+d.getLocation().getLine()+",\"startColumn\":"+d.getLocation().getColumn()+"}}],\"value\":{}}");return;}
 RadarChartDiagram d=(RadarChartDiagram)((MermaidParseResult.Success)result).getDiagram();List<String> axes=new ArrayList<>(),curves=new ArrayList<>();
 for(RadarAxis a:d.getAxes())axes.add("{\"$type\":\"Axis\",\"name\":"+q(a.getId())+",\"label\":"+q(a.getLabel())+"}");
 for(RadarCurve c:d.getCurves()){List<String> entries=new ArrayList<>();for(RadarEntry v:c.getEntries())entries.add("{\"$type\":\"Entry\",\"value\":"+v.getValue()+(v.getAxis()==null?"":",\"axis\":{\"$refText\":"+q(v.getAxis())+"}")+"}");curves.add("{\"$type\":\"Curve\",\"name\":"+q(c.getId())+",\"label\":"+q(c.getLabel())+",\"entries\":["+String.join(",",entries)+"]}");}
 List<String> options=new ArrayList<>();for(RadarOption o:d.getOptions())options.add("{\"$type\":\"Option\",\"name\":"+q(o.getName())+",\"value\":"+(o.getNumber()!=null?o.getNumber():o.getFlag()!=null?o.getFlag():q(o.getText()))+"}");
 System.out.println("{\"lexerErrors\":[],\"parserErrors\":[],\"value\":{\"$type\":\"Radar\",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescr\":"+q(d.getAccessibilityDescription())+",\"title\":"+q(d.getTitle())+",\"axes\":["+String.join(",",axes)+"],\"curves\":["+String.join(",",curves)+"],\"options\":["+String.join(",",options)+"]}}");
 }
}
