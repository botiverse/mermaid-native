package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class IshikawaHierarchyLayoutTest {
    private fun scene(source: String): LayoutScene {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse("ishikawa-beta\n$source")).diagram
        return SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
    }

    private fun assertConnectsToParent(scene: LayoutScene, child: String, parent: String) {
        val labels = scene.commands.filterIsInstance<DrawText>().associateBy { it.text }
        fun baseline(label: String) = labels.getValue(label).let { it.origin.y - it.style.fontSize * 0.4 }
        val lines = scene.commands.filterIsInstance<DrawLine>()
        val parentY = baseline(parent)
        val parentBranch = lines.single { abs(it.from.y-parentY)<0.001 && abs(it.to.y-parentY)<0.001 }
        val connector = lines.single { abs(it.from.y-baseline(child))<0.001 && abs(it.to.y-parentY)<0.001 }
        assertTrue(connector.to.x > minOf(parentBranch.from.x,parentBranch.to.x))
        assertTrue(connector.to.x < maxOf(parentBranch.from.x,parentBranch.to.x))
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().any { p -> p.points.any { abs(it.x-connector.to.x)<0.001 && abs(it.y-connector.to.y)<0.001 } }, "Arrow must terminate at the parent branch")
    }

    @Test fun nestedCausesConnectToTheirOwnParentOnBothSides() {
        val result = scene("Blurry Photo\n    Process\n        Focus\n            Wrong setting\n    Equipment\n        LENS\n            Dirty lens\n        SENSOR\n            Damaged sensor")
        assertConnectsToParent(result,"Wrong setting","Focus")
        assertConnectsToParent(result,"Dirty lens","LENS")
        assertConnectsToParent(result,"Damaged sensor","SENSOR")
    }

    @Test fun deepCausesRetainEveryParentAndFitMeasuredLabels() {
        val result = scene("Failure\n    Equipment\n        Lens\n            Dust\n                Cleaning\n                    Training with a long measured label")
        assertConnectsToParent(result,"Dust","Lens")
        assertConnectsToParent(result,"Cleaning","Dust")
        assertConnectsToParent(result,"Training with a long measured label","Cleaning")
        for (text in result.commands.filterIsInstance<DrawText>()) {
            val width=FixedWidthTextMeasurer.measure(text.text,text.style).width
            val left=when(text.anchor) { TextAnchor.END -> text.origin.x-width;TextAnchor.MIDDLE -> text.origin.x-width/2;else -> text.origin.x }
            assertTrue(left>=0,"${text.text} starts outside the scene")
            assertTrue(left+width<=result.width,"${text.text} ends outside the scene")
            assertTrue(text.origin.y>=0 && text.origin.y<=result.height)
        }
    }
}
