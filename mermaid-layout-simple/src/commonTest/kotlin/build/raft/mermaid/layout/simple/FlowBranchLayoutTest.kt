package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class FlowBranchLayoutTest {
    private fun scene(source:String)=SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,FixedWidthTextMeasurer,LayoutConfig())

    @Test fun decisionBranchesStayDistinctInEveryDirectionAndConvergeAfterBothTargets() {
        for(direction in FlowDirection.entries){
            val source="flowchart ${direction.name}\nA[Start] --> B{Ready?}\nB -->|yes| C[Ship]\nB -->|no| D[Fix]\nC --> E[Finish]\nD --> E"
            val result=scene(source)
            val labels=result.commands.filterIsInstance<DrawText>().associateBy { it.text }
            val ship=labels.getValue("Ship").origin;val fix=labels.getValue("Fix").origin
            val finish=labels.getValue("Finish").origin;val decision=labels.getValue("Ready?").origin
            if(direction in listOf(FlowDirection.LR,FlowDirection.RL)){
                assertEquals(ship.x,fix.x,direction.name);assertTrue(abs(ship.y-fix.y)>=80,direction.name)
                assertTrue((ship.x-decision.x)*(finish.x-ship.x)>0,direction.name)
            }else{
                assertEquals(ship.y,fix.y,direction.name);assertTrue(abs(ship.x-fix.x)>=120,direction.name)
                assertTrue((ship.y-decision.y)*(finish.y-ship.y)>0,direction.name)
            }
            // Every visible connection stays outside unrelated rectangle interiors.
            val rectangles=result.commands.filterIsInstance<DrawRect>().map { it.rect }
            result.commands.filterIsInstance<DrawLine>().forEach { edge ->
                for(step in 1..99){
                    val t=step/100.0;val x=edge.from.x+(edge.to.x-edge.from.x)*t;val y=edge.from.y+(edge.to.y-edge.from.y)*t
                    assertFalse(rectangles.any { x>it.x+.01 && x<it.x+it.width-.01 && y>it.y+.01 && y<it.y+it.height-.01 },"${direction.name}: edge crosses a node at $x,$y")
                }
            }
            assertEquals(result,scene(source))
        }
    }

    @Test fun edgeDirectionDeterminesRanksEvenWhenTargetsWereDeclaredFirst() {
        val result=scene("flowchart TD\nC[Ship]\nD[Fix]\nB{Ready?}\nA[Start]\nA --> B\nB --> C\nB --> D")
        val labels=result.commands.filterIsInstance<DrawText>().associateBy { it.text }
        assertTrue(labels.getValue("Start").origin.y<labels.getValue("Ready?").origin.y)
        assertTrue(labels.getValue("Ready?").origin.y<labels.getValue("Ship").origin.y)
        assertEquals(labels.getValue("Ship").origin.y,labels.getValue("Fix").origin.y)
    }

    @Test fun diamondContainsMultilineTextAndDrawsItsStyledOutline() {
        val result=scene("flowchart TB\nA{A long decision<br/>with two lines}\nstyle A stroke:#123456,stroke-width:3px")
        val polygon=result.commands.filterIsInstance<DrawPolygon>().single();val outline=result.commands.filterIsInstance<DrawPolyline>().single()
        val left=polygon.points.minOf { it.x };val top=polygon.points.minOf { it.y }
        val width=polygon.points.maxOf { it.x }-left;val height=polygon.points.maxOf { it.y }-top
        assertEquals(width,height);assertEquals("#123456",outline.stroke.value);assertEquals(3.0,outline.strokeWidth)
        result.commands.filterIsInstance<DrawText>().forEach { text ->
            val measured=FixedWidthTextMeasurer.measure(text.text,text.style)
            val halfWidth=measured.width/2
            val halfHeight=abs(text.origin.y-text.style.fontSize*.35-(top+height/2))+text.style.fontSize/2
            assertTrue(halfWidth/(width/2)+halfHeight/(height/2)<1,"Text rectangle must fit inside diamond")
        }
    }

    @Test fun nestedBranchesRespectContainerDirectionAndBounds() {
        val result=scene("flowchart LR\nsubgraph G[Choices]\ndirection TB\nA{Ready?} --> B[Ship]\nA --> C[Fix]\nend\nG --> D[Next]")
        val labels=result.commands.filterIsInstance<DrawText>().associateBy { it.text }
        assertEquals(labels.getValue("Ship").origin.y,labels.getValue("Fix").origin.y)
        assertTrue(labels.getValue("Ship").origin.x<labels.getValue("Fix").origin.x)
        val branchEdges=result.commands.filterIsInstance<DrawLine>().take(2)
        assertTrue(branchEdges.all { it.from.y<it.to.y },"Internal edges must follow the group's TB direction")
        assertEquals(branchEdges[0].from,branchEdges[1].from,"Both branches leave the bottom diamond tip")
        val group=result.commands.filterIsInstance<DrawRect>().first().rect
        for(name in listOf("Ready?","Ship","Fix")){
            val p=labels.getValue(name).origin
            assertTrue(p.x>group.x && p.x<group.x+group.width && p.y>group.y && p.y<group.y+group.height)
        }
    }
}
