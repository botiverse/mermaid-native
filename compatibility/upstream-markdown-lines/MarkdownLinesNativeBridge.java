import build.raft.mermaid.layout.simple.MermaidMarkdown;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/** Encoding and public-field projection only; Kotlin tokenizes the source. */
public final class MarkdownLinesNativeBridge {
  static String quote(String s) {
    var out = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      if (c == '"' || c == '\\') out.append('\\').append(c);
      else if (c < 32) out.append(String.format("\\u%04x", (int)c));
      else out.append(c);
    }
    return out.append('"').toString();
  }
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String input; (input = reader.readLine()) != null;) {
      var source = new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
      var lines = MermaidMarkdown.INSTANCE.lines(source);
      var out = new StringBuilder("[");
      for (int i=0; i<lines.size(); i++) {
        if (i>0) out.append(','); out.append('[');
        var words=lines.get(i);
        for (int k=0; k<words.size(); k++) {
          if (k>0) out.append(','); var word=words.get(k);
          out.append("{\"content\":").append(quote(word.getContent())).append(",\"type\":")
            .append(quote(word.getType().name().toLowerCase(Locale.ROOT))).append('}');
        }
        out.append(']');
      }
      System.out.println(out.append(']'));
    }
  }
}
