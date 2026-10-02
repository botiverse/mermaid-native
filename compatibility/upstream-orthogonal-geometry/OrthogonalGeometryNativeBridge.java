import build.raft.mermaid.layout.ScenePoint;
import build.raft.mermaid.layout.SceneRect;
import build.raft.mermaid.layout.simple.OrthogonalGeometry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Numeric and identity transport only. Kotlin owns every geometry decision. */
public final class OrthogonalGeometryNativeBridge {
  private static final class Input {
    final String[] fields; int i;
    Input(String line) { fields=line.split("\\t",-1); }
    double number() { return Double.parseDouble(fields[i++]); }
    int integer() { return Integer.parseInt(fields[i++]); }
    String text() { return new String(Base64.getDecoder().decode(fields[i++]),StandardCharsets.UTF_8); }
    ScenePoint point() { return new ScenePoint(number(),number()); }
  }
  public static void main(String[] args) throws Exception {
    var reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
    for(String line;(line=reader.readLine())!=null;) {
      var in=new Input(line);int op=in.integer();var a=in.point();var b=in.point();boolean result;
      var geometry=OrthogonalGeometry.INSTANCE;
      if(op==0 || op==1 || op==4) {
        var c=in.point();var d=in.point();double epsilon=in.number();
        result=op==0?geometry.segmentsCross(a,b,c,d,epsilon,in.number()):op==4?geometry.segmentsStrictlyCross(a,b,c,d,epsilon):geometry.sameAxisSegmentsOverlap(a,b,c,d,epsilon);
      } else if(op==2) {
        int count=in.integer();var rects=new ArrayList<OrthogonalGeometry.RectEntry>();
        for(int i=0;i<count;i++)rects.add(new OrthogonalGeometry.RectEntry(in.text(),new SceneRect(in.number(),in.number(),in.number(),in.number())));
        int n=in.integer();var excluded=new ArrayList<String>();for(int i=0;i<n;i++)excluded.add(in.text());
        result=geometry.segmentHitsAnyRect(a,b,rects,excluded,in.number());
      } else if(op==3) {
        double epsilon=in.number();boolean skip=in.integer()!=0;int count=in.integer();int excluded=in.integer();
        var edges=new ArrayList<OrthogonalGeometry.Edge>();
        for(int i=0;i<count;i++) {boolean layoutOnly=in.integer()!=0;int n=in.integer();var points=new ArrayList<ScenePoint>();
          for(int j=0;j<n;j++)points.add(in.point());edges.add(new OrthogonalGeometry.Edge(points,layoutOnly));}
        result=geometry.segmentConflictsWithAnyEdge(a,b,edges,excluded<0?null:edges.get(excluded),epsilon,skip);
      } else throw new IllegalArgumentException("unknown geometry operation");
      if(in.i!=in.fields.length)throw new IllegalArgumentException("unconsumed transport fields");
      System.out.println("{\"value\":"+result+"}");
    }
  }
}
