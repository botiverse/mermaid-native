import build.raft.mermaid.core.RailroadSyntax;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Only transports detector inputs; all classification runs in production Kotlin. */
public final class RailroadDetectionNativeBridge {
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      String[] fields = line.split("\t", -1);
      String source = new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8);
      System.out.println(RailroadSyntax.valueOf(fields[0]).matchesSource(source));
    }
  }
}
