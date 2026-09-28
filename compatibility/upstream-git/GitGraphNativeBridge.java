import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class GitGraphNativeBridge {
 static String q(String s) { if(s==null)return "null"; StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString(); }


 public static void main(String[] args){Scanner lines=new Scanner(System.in,StandardCharsets.UTF_8);while(lines.hasNextLine())render(new String(Base64.getDecoder().decode(lines.nextLine()),StandardCharsets.UTF_8));}
 static void render(String source){
 MermaidParseResult result=MermaidParser.INSTANCE.parse(source);if(result instanceof MermaidParseResult.Failure){System.out.println("{\"error\":"+q(((MermaidParseResult.Failure)result).getDiagnostics().get(0).getMessage())+"}");return;}
 MermaidDiagram diagram=((MermaidParseResult.Success)result).getDiagram();if(!(diagram instanceof GitGraphDiagram)){System.out.println("{\"error\":\"Another diagram type\"}");return;}GitGraphDiagram d=(GitGraphDiagram)diagram;List<String> commits=new ArrayList<>(),branches=new ArrayList<>(),warnings=new ArrayList<>();int seq=0;
 for(GitGraphCommit c:d.getCommits()){List<String> parents=new ArrayList<>(),tags=new ArrayList<>();for(String id:c.getParentIds())parents.add(q(id));for(String tag:c.getTags())tags.add(q(tag));commits.add("{\"id\":"+q(c.getId())+",\"branch\":"+q(c.getBranch())+",\"parents\":["+String.join(",",parents)+"],\"message\":"+q(c.getMessage())+",\"type\":"+(c.isCherryPick()?4:c.isMerge()?3:c.getType().ordinal())+",\"customId\":"+c.getCustomId()+",\"customType\":"+(c.getCustomType()==null?"null":c.getCustomType().ordinal())+",\"seq\":"+c.getSequence()+",\"tags\":["+String.join(",",tags)+"]}");}
 for(GitGraphBranch b:d.getBranches())branches.add("{\"name\":"+q(b.getName())+",\"order\":"+b.getOrder()+",\"head\":"+q(d.getBranchHeads().get(b.getName()))+"}");for(String warning:d.getWarnings())warnings.add(q(warning));
 System.out.println("{\"direction\":"+q(d.getDirection().name())+",\"currentBranch\":"+q(d.getCurrentBranch())+",\"head\":"+q(d.getBranchHeads().get(d.getCurrentBranch()))+",\"accTitle\":"+q(d.getAccessibilityTitle())+",\"accDescription\":"+q(d.getAccessibilityDescription())+",\"warnings\":["+String.join(",",warnings)+"],\"branches\":["+String.join(",",branches)+"],\"commits\":["+String.join(",",commits)+"]}");
 }
}
