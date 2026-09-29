import java.nio.charset.StandardCharsets;
import java.util.*;
import build.raft.mermaid.core.*;

public class XyModelNativeBridge {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in, StandardCharsets.UTF_8);
        while (input.hasNextLine()) {
            String source = new String(Base64.getDecoder().decode(input.nextLine()), StandardCharsets.UTF_8);
            MermaidParseResult result = MermaidParser.INSTANCE.parse(source);
            if (result instanceof MermaidParseResult.Failure) {
                System.out.println("{\"error\":" + XyNativeBridge.q(((MermaidParseResult.Failure)result).getDiagnostics().toString()) + "}");
                continue;
            }
            XyChartDiagram d = (XyChartDiagram)((MermaidParseResult.Success)result).getDiagram();
            List<String> plots = new ArrayList<>();
            for (XySeries series : d.getSeries()) {
                List<Double> values = series.valuesFor(d.getXAxis());
                List<String> points = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) {
                    String category = d.getXAxis().getCategories().isEmpty()
                        ? Integer.toString(i + 1) : XyNativeBridge.q(d.getXAxis().getCategories().get(i));
                    points.add("[" + category + "," + values.get(i) + "]");
                }
                plots.add("{\"type\":" + XyNativeBridge.q(series.getKind().name().toLowerCase(Locale.ROOT)) +
                    ",\"title\":" + XyNativeBridge.q(series.getDisplayTitle()) + ",\"data\":[" + String.join(",", points) + "]}");
            }
            System.out.println("{\"plots\":[" + String.join(",", plots) + "],\"yAxis\":{\"type\":\"linear\",\"min\":" +
                d.getYAxis().getMinimum() + ",\"max\":" + d.getYAxis().getMaximum() + "}}");
        }
    }
}
