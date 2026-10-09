package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class SwimlaneObstacleRoutesTest {
    private fun checked(a:SceneRect,b:SceneRect,obstacles:List<SceneRect>,previous:List<List<ScenePoint>> = emptyList()):List<ScenePoint> {
        val path=assertNotNull(swimlaneOrthogonalPath(a,b,obstacles,previous))
        assertTrue(path.size>=2)
        for((p,q) in path.zipWithNext()) {
            assertTrue(abs(p.x-q.x)<1e-6 || abs(p.y-q.y)<1e-6)
            for(r in obstacles+a+b) assertFalse(segmentEntersRect(p,q,r),"$p -> $q enters $r")
        }
        fun border(p:ScenePoint,r:SceneRect)=p.x in r.x..r.x+r.width && p.y in r.y..r.y+r.height &&
            (abs(p.x-r.x)<1e-6 || abs(p.x-r.x-r.width)<1e-6 || abs(p.y-r.y)<1e-6 || abs(p.y-r.y-r.height)<1e-6)
        assertTrue(border(path.first(),a));assertTrue(border(path.last(),b));return path
    }
    @Test fun routesAroundInterveningNodeInAllDirections() {
        var a=SceneRect(0.0,0.0,80.0,40.0);var b=SceneRect(300.0,0.0,80.0,40.0);var obstacle=SceneRect(140.0,-30.0,100.0,100.0)
        fun rotate(r:SceneRect)=SceneRect(-r.y-r.height,r.x,r.height,r.width)
        repeat(4){checked(a,b,listOf(a,b,obstacle));a=rotate(a);b=rotate(b);obstacle=rotate(obstacle)}
    }
    @Test fun directClearAlignedConnectionStaysStraight() {
        val a=SceneRect(0.0,0.0,80.0,40.0);val b=SceneRect(180.0,0.0,80.0,40.0)
        assertEquals(listOf(ScenePoint(80.0,20.0),ScenePoint(180.0,20.0)),checked(a,b,listOf(a,b)))
    }
    @Test fun avoidsHeadingEvenWhenOnlyOneSideIsOpen() {
        val a=SceneRect(0.0,50.0,80.0,40.0);val b=SceneRect(200.0,160.0,80.0,40.0)
        checked(a,b,listOf(a,b,SceneRect(-30.0,100.0,300.0,30.0)))
    }
    @Test fun repeatedEndpointsCanUseDifferentTracks() {
        val a=SceneRect(0.0,0.0,80.0,40.0);val b=SceneRect(180.0,0.0,80.0,40.0)
        val first=checked(a,b,listOf(a,b));val second=checked(a,b,listOf(a,b),listOf(first));assertNotEquals(first,second)
    }
    @Test fun diagonalHandoffUsesFacingPortsRatherThanIncomingSide() {
        val a=SceneRect(300.0,0.0,80.0,40.0);val b=SceneRect(180.0,200.0,80.0,40.0)
        val route=checked(a,b,listOf(a,b))
        assertEquals(ScenePoint(340.0,40.0),route.first())
        assertEquals(ScenePoint(220.0,200.0),route.last())
    }
    @Test fun invalidAndOversizedGeometryIsBounded() {
        assertNull(swimlaneOrthogonalPath(SceneRect(Double.NaN,0.0,80.0,40.0),SceneRect(0.0,0.0,80.0,40.0),emptyList()))
        assertNull(swimlaneOrthogonalPath(SceneRect(0.0,0.0,80.0,40.0),SceneRect(9000.0,0.0,80.0,40.0),List(300){SceneRect(it*100.0,it*100.0,80.0,40.0)}))
    }
    @Test fun actualSwimlanePathsAvoidNodesAndTitlesAcrossDirections() {
        for(direction in listOf("TB","BT","LR","RL")) {
            val parsed=assertIs<SwimlaneDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("swimlane-beta $direction\nsubgraph One\nA-->B-->C\nend\nsubgraph Middle\nX-->Y\nend\nsubgraph Three\nD-->E\nend\nA-->E\nC-->D")).diagram)
            val diagram=swimlaneFlow(parsed)
            val placement=placeSwimlanes(diagram,diagram.nodes.associate { it.id to SceneSize(80.0,40.0) },LayoutConfig(),FixedWidthTextMeasurer)
            val routes=swimlaneObstacleRoutes(diagram,placement.placement.nodes,placement.placement.groups,placement.titles,emptyMap(),FixedWidthTextMeasurer)
            assertEquals(diagram.edges.size,routes.size)
            for(route in routes.values)for((a,b) in route.points.zipWithNext())for(box in placement.placement.nodes.values+placement.titles.values)
                assertFalse(segmentEntersRect(a,b,box),"$direction: $a->$b crosses $box")
            val scene=SimpleMermaidLayout.layout(parsed,FixedWidthTextMeasurer,LayoutConfig(validateOrthogonalLayout=true))
            val geometry=assertNotNull(scene.layoutValidation).geometry
            for(edge in geometry.edges) for((a,b) in edge.points.zipWithNext()) for(node in geometry.nodes.filter { !it.isGroup })
                assertFalse(segmentEntersRect(a,b,node.bounds),"rendered $direction ${edge.id} enters ${node.id}")
            assertTrue(scene.commands.filterIsInstance<DrawPolyline>().isNotEmpty())
            assertTrue(scene.width.isFinite() && scene.height.isFinite())
        }
    }
}
