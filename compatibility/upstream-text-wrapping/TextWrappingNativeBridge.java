import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;
public class TextWrappingNativeBridge {
 static String q(String text){return RailroadNativeBridge.q(text);}
 static String decode(String text){return new String(Base64.getDecoder().decode(text),StandardCharsets.UTF_8);}
 static String list(List<String> values){return "["+String.join(",",values.stream().map(TextWrappingNativeBridge::q).toList())+"]";}
 public static void main(String[] args){
  Scanner scanner=new Scanner(System.in,StandardCharsets.UTF_8);
  while(scanner.hasNextLine())try{
   String[] fields=scanner.nextLine().split("\t",-1);boolean graphemes=fields[1].equals("1");
   if(fields[0].equals("split")){
    String text=decode(fields[2]);System.out.println(list(graphemes?UnicodeGraphemes.INSTANCE.split(text):UnicodeGraphemes.INSTANCE.codePoints(text)));continue;
   }
   int width=Integer.parseInt(fields[2]),count=Integer.parseInt(fields[3]);List<MermaidWord> words=new ArrayList<>();
   for(int i=0;i<count;i++)words.add(new MermaidWord(decode(fields[4+i*2]),decode(fields[5+i*2])));
   var result=MermaidTextWrapping.INSTANCE.wrapLine(words,candidate->{
    String text=String.join("",candidate.stream().map(MermaidWord::getContent).toList());
    return (graphemes?UnicodeGraphemes.INSTANCE.split(text):UnicodeGraphemes.INSTANCE.codePoints(text)).size()<=width;
   },graphemes);
   List<String> lines=new ArrayList<>();for(var line:result){List<String> values=new ArrayList<>();for(var word:line)values.add("{\"content\":"+q(word.getContent())+",\"type\":"+q(word.getType())+"}");lines.add("["+String.join(",",values)+"]");}
   System.out.println("["+String.join(",",lines)+"]");
  }catch(Throwable e){System.out.println("{\"error\":"+q(e.getMessage()==null?e.getClass().getName():e.getMessage())+"}");}
 }
}
