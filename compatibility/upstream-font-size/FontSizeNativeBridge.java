import build.raft.mermaid.core.FontSize;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Only transports typed inputs to the production parser and serializes its result. */
public final class FontSizeNativeBridge {
  public static void main(String[] args) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      String[] fields = line.split("\t", -1);
      String raw = new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8);
      Object input;
      switch (fields[0]) {
        case "number": input = Double.valueOf(raw); break;
        case "string": input = raw; break;
        case "undefined": input = null; break;
        default: input = new Object(); // Unsupported non-primitive input, rejected inside Kotlin.
      }
      FontSize size = FontSize.Companion.parse(input);
      if (size == null) System.out.println("null");
      else System.out.println(size.getValue() + "\t" + Base64.getEncoder().encodeToString(size.getCssValue().getBytes(StandardCharsets.UTF_8)));
    }
  }
}
