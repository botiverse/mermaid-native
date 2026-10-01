import build.raft.mermaid.layout.ScenePoint;
import build.raft.mermaid.layout.simple.MermaidPathGeometry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Numeric transport only. Production Kotlin owns traversal and rounding. */
public final class PathPointNativeBridge {
  public static void main(String[] args) throws Exception {
    var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      var values = line.split("\\t");
      var points = new ArrayList<ScenePoint>();
      for (int i = 1; i < values.length; i += 2)
        points.add(new ScenePoint(Double.parseDouble(values[i]), Double.parseDouble(values[i+1])));
      try {
        var p = MermaidPathGeometry.INSTANCE.pointAt(points, Double.parseDouble(values[0]));
        System.out.println("{\"point\":{\"x\":" + p.getX() + ",\"y\":" + p.getY() + "}}");
      } catch (IllegalStateException e) {
        // Only the documented domain error is transported. Other failures abort the run.
        if (!"Could not find a suitable point for the given distance".equals(e.getMessage())) throw e;
        System.out.println("{\"error\":\"Could not find a suitable point for the given distance\"}");
      }
    }
  }
}
