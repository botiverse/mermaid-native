import build.raft.mermaid.core.MermaidParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** String transport only; the production parser owns all type detection. */
public final class TypeDetectionNativeBridge {
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      var source = new String(Base64.getDecoder().decode(line), StandardCharsets.UTF_8);
      var type = MermaidParser.INSTANCE.detectType(source);
      if (type == null) throw new IllegalArgumentException("No Native diagram type");
      System.out.println(Base64.getEncoder().encodeToString(type.getId().getBytes(StandardCharsets.UTF_8)));
    }
  }
}
