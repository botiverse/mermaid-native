import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

/** Nested JSON is a projection of Kotlin parentIndex, never an indentation parser. */
public class TreeViewModelNativeBridge {
    static String q(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c);
            }
        }
        return out.append('"').toString();
    }

    static String children(List<TreeViewNode> nodes, Integer parent) {
        List<String> result = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            TreeViewNode n = nodes.get(index);
            if (!Objects.equals(n.getParentIndex(), parent)) continue;
            result.add("{\"name\":" + q(n.getLabel()) +
                ",\"nodeType\":" + q(n.getDirectory() ? "directory" : "file") +
                ",\"level\":" + (n.getSourceIndent() == null ? 0 : n.getSourceIndent()) +
                ",\"cssClass\":" + q(n.getClassAnnotation()) +
                ",\"icon\":" + q(n.getIconAnnotation()) +
                ",\"description\":" + q(n.getDescription()) +
                ",\"children\":" + children(nodes, index) + "}");
        }
        return "[" + String.join(",", result) + "]";
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in, StandardCharsets.UTF_8);
        while (input.hasNextLine()) {
            String source = new String(Base64.getDecoder().decode(input.nextLine()), StandardCharsets.UTF_8);
            MermaidParseResult parsed = MermaidParser.INSTANCE.parse(source);
            if (parsed instanceof MermaidParseResult.Failure) {
                System.out.println("{\"error\":" + q(((MermaidParseResult.Failure)parsed).getDiagnostics().get(0).getMessage()) + "}");
                continue;
            }
            TreeViewDiagram d = (TreeViewDiagram)((MermaidParseResult.Success)parsed).getDiagram();
            System.out.println("{\"root\":{\"children\":" + children(d.getNodes(), null) +
                "},\"title\":" + q(d.getTitle()) + ",\"accTitle\":" + q(d.getAccTitle()) +
                ",\"accDescription\":" + q(d.getAccDescription()) + "}");
        }
    }
}
