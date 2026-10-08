import java.util.*;
import build.raft.mermaid.layout.simple.MaterializedJogsKt;
public class FixedKeyBridge {
 public static void main(String[] args) {
  var in=new Scanner(System.in);while(in.hasNextLine())System.out.println(MaterializedJogsKt.materializedFixed3Key(Double.parseDouble(in.nextLine())));
 }
}
