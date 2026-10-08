package build.raft.mermaid.web

import build.raft.mermaid.layout.*
import kotlin.math.PI
import kotlin.test.*

class ScenePathTest {
    @Test fun canvasWireRetainsCommandsAndNegativeSweep() {
        val p = DrawPath(listOf(PathMove(ScenePoint(1.0, 2.0)), PathQuadratic(ScenePoint(3.0, 4.0), ScenePoint(5.0, 6.0)), PathMove(ScenePoint(8.0, 6.0)), PathArc(ScenePoint(10.0, 6.0), 2.0, PI, -PI)), pattern = StrokePattern.DASHED)
        val script = MermaidCanvasRenderer.render(LayoutScene(20.0, 20.0, listOf(p)))
        assertTrue(script.contains("[\"M\",1,2],[\"Q\",3,4,5,6],[\"M\",8,6],[\"A\",10,6,2,"))
        assertTrue(script.contains((-PI).toString()))
        assertTrue(script.contains("\"dash\":true"))
    }
}
