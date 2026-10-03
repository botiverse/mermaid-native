import build.raft.mermaid.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** One production parse per original invocation; only transport status and diagnostics. */
public final class ExampleParseNativeBridge {
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      var source = new String(Base64.getDecoder().decode(line), StandardCharsets.UTF_8);
      var result = MermaidParser.INSTANCE.parse(source);
      String value;
      if (result instanceof MermaidParseResult.Success) value = "PASS";
      else {
        var diagnostics = ((MermaidParseResult.Failure) result).getDiagnostics();
        value = "FAIL\n" + diagnostics.toString();
      }
      System.out.println(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
    }
  }
}
