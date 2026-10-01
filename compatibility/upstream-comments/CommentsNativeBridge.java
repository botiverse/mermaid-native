import build.raft.mermaid.core.MermaidComments;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Base64 transport only; production Kotlin performs all comment cleanup. */
public final class CommentsNativeBridge {
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      var source = new String(Base64.getDecoder().decode(line), StandardCharsets.UTF_8);
      var result = MermaidComments.INSTANCE.cleanup(source);
      System.out.println(Base64.getEncoder().encodeToString(result.getBytes(StandardCharsets.UTF_8)));
    }
  }
}
