import build.raft.mermaid.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Transports test fields; selection and qualification run only in production Kotlin. */
public final class TreeViewIconsNativeBridge {
  static String decode(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
  static String quote(String s) {
    if (s == null) return "null";
    var out = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      if (c == '"' || c == '\\') out.append('\\').append(c);
      else if (c < 32) out.append(String.format("\\u%04x", (int)c));
      else out.append(c);
    }
    return out.append('"').toString();
  }
  static Map<String,String> readMap(BufferedReader reader, int count) throws IOException {
    var result = new LinkedHashMap<String,String>();
    for (int i=0; i<count; i++) {
      var pair=reader.readLine().split("\t",-1); result.put(decode(pair[0]),decode(pair[1]));
    }
    return result;
  }
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line=reader.readLine()) != null;) {
      var f=line.split("\t",-1);
      var config=new TreeViewIconConfig(Boolean.parseBoolean(f[4]),decode(f[5]),readMap(reader,Integer.parseInt(f[6])),readMap(reader,Integer.parseInt(f[7])));
      String result;
      if (f[0].equals("detectIcon")) result=TreeViewIcons.INSTANCE.detectIcon(decode(f[1]),config);
      else if (f[0].equals("getNodeIcon")) {
        var node=new TreeViewNode(decode(f[1]),0,null,Boolean.parseBoolean(f[2]),null,null,f[3].equals("-") ? null : decode(f[3]),null);
        result=TreeViewIcons.INSTANCE.getNodeIcon(node,config);
      } else throw new IllegalArgumentException("Unknown method");
      System.out.println(quote(result));
    }
  }
}
