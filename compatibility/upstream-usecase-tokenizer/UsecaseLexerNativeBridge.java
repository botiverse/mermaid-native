import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Transport raw input to the production Kotlin tokenizer; no lexical work lives here. */
public class UsecaseLexerNativeBridge {
 static String q(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
 static String json(Object o){if(o==null)return "null";if(o instanceof String)return q((String)o);if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){List<String>a=new ArrayList<>();((Map<?,?>)o).forEach((k,v)->a.add(q(k.toString())+":"+json(v)));return "{"+String.join(",",a)+"}";}if(o instanceof Iterable){List<String>a=new ArrayList<>();for(Object v:(Iterable<?>)o)a.add(json(v));return "["+String.join(",",a)+"]";}throw new IllegalArgumentException(o.getClass().getName());}
 public static void main(String[] args){
  Scanner in=new Scanner(System.in,StandardCharsets.UTF_8);
  while(in.hasNextLine()){
   String source=new String(Base64.getDecoder().decode(in.nextLine()),StandardCharsets.UTF_8);
   UsecaseLexResult r=UsecaseLexer.INSTANCE.tokenize(source);List<Object> tokens=new ArrayList<>(),errors=new ArrayList<>();
   for(UsecaseToken t:r.getTokens())tokens.add(Map.of("name",t.getName(),"image",t.getImage(),"startOffset",t.getStartOffset(),"endOffset",t.getEndOffset(),"startLine",t.getStartLine(),"startColumn",t.getStartColumn(),"endLine",t.getEndLine(),"endColumn",t.getEndColumn()));
   for(UsecaseLexError e:r.getErrors())errors.add(Map.of("offset",e.getOffset(),"length",e.getLength(),"line",e.getLine(),"column",e.getColumn()));
   System.out.println(json(Map.of("tokens",tokens,"errors",errors)));
  }
 }
}
