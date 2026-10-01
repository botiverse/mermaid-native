import build.raft.mermaid.core.TreemapHierarchy;
import build.raft.mermaid.core.TreemapHierarchyItem;
import build.raft.mermaid.core.TreemapNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Transports flat rows and serializes the actual Kotlin hierarchy without building it. */
public final class TreemapHierarchyNativeBridge {
  static String decode(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
  static String quote(String s) {
    var out = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      if (c == '"' || c == '\\') out.append('\\').append(c);
      else if (c < 32) out.append(String.format("\\u%04x", (int)c));
      else out.append(c);
    }
    return out.append('"').toString();
  }
  static String serialize(List<TreemapNode> nodes) {
    var out = new StringJoiner(",", "[", "]");
    for (var node : nodes) {
      String row = "{\"name\":" + quote(node.getLabel());
      if (node.getValue() == null) row += ",\"children\":" + serialize(node.getChildren());
      else row += ",\"value\":" + node.getValue();
      if (node.getClassSelector() != null) row += ",\"classSelector\":" + quote(node.getClassSelector());
      out.add(row + "}");
    }
    return out.toString();
  }
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      var hierarchy = new TreemapHierarchy();
      int count = Integer.parseInt(line);
      for (int i = 0; i < count; i++) {
        String[] row = reader.readLine().split("\t", -1);
        hierarchy.add(new TreemapHierarchyItem(Integer.parseInt(row[0]), decode(row[1]),
          row[2].isEmpty() ? null : Double.valueOf(row[2]), row[3].isEmpty() ? null : decode(row[3])));
      }
      System.out.println(serialize(hierarchy.build()));
    }
  }
}
