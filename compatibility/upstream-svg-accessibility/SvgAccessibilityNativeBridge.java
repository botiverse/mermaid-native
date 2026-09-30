import build.raft.mermaid.layout.LayoutScene;
import build.raft.mermaid.render.svg.SvgRenderer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;

/** Only serializes supplied metadata through the production SVG renderer. */
public final class SvgAccessibilityNativeBridge {
  static String decode(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
  public static void main(String[] args) throws Exception {
    Method configured = null;
    try { configured = SvgRenderer.class.getMethod("render", LayoutScene.class, String.class, String.class); }
    catch (NoSuchMethodException oldRuntime) { /* Baseline: old serializer has no ID/type API. */ }
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    for (String line; (line = reader.readLine()) != null;) {
      String[] a = line.split("\t", -1);
      String title = decode(a[0]), description = decode(a[1]), id = decode(a[2]), type = decode(a[3]);
      LayoutScene scene = new LayoutScene(120.0, 80.0, Collections.emptyList(), title.isEmpty() ? null : title, description.isEmpty() ? null : description);
      String svg = configured == null ? SvgRenderer.INSTANCE.render(scene) : (String) configured.invoke(SvgRenderer.INSTANCE, scene, id, type);
      System.out.println(Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)));
    }
  }
}
