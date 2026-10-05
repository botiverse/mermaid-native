import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Calls the production Native document boundary; JSON is transport only. */
public class FrontmatterBridge {
 public static void main(String[] args) {
  Scanner input = new Scanner(System.in, StandardCharsets.UTF_8);
  while(input.hasNextLine()) {
   String text = new String(Base64.getDecoder().decode(input.nextLine()),StandardCharsets.UTF_8);
   try {
    var doc = MermaidFrontmatter.INSTANCE.extract(text);
    var m = doc.getMetadata(); Map<String,Object> metadata = new LinkedHashMap<>();
    if(m.getTitle()!=null)metadata.put("title",m.getTitle());
    if(m.getDisplayMode()!=null)metadata.put("displayMode",m.getDisplayMode());
    if(m.getConfig()!=null)metadata.put("config",UsecaseDocumentBridge.unpack(m.getConfig()));
    System.out.println(TreeDocumentsBridge.json(Map.of("value",Map.of("text",doc.getText(),"metadata",metadata))));
   } catch(MermaidFrontmatterError e) { System.out.println(TreeDocumentsBridge.json(Map.of("error",e.getMessage()))); }
  }
 }
}
