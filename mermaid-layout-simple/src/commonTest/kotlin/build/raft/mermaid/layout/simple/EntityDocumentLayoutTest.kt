package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class EntityDocumentLayoutTest {
    @Test fun editedNestedGroupsStylesAndClusterEdgesReachTheRealRenderer() {
        val doc = EntityRelationshipDocument()
        doc.addEntity("A"); doc.addEntity("B")
        doc.addSubgraph("inner", listOf("B"), "Inner")
        doc.addSubgraph("outer", listOf("A", "inner"), "Outer")
        doc.addStyles(listOf("outer"), listOf("fill:red"))
        doc.addRelationship("outer", "owns", "B", EntityCardinality.ONLY_ONE, EntityCardinality.ZERO_OR_MORE)
        val source = "erDiagram\nsubgraph outer[Outer]\nA\nsubgraph inner[Inner]\nB\nend\nend\nstyle outer fill:red\nouter ||--o{ B : owns"
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        val actual = SimpleMermaidLayout.layout(doc.diagram(), FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig()), actual)
        val snapshot = doc.diagram(); doc.clear()
        assertEquals(actual, SimpleMermaidLayout.layout(snapshot, FixedWidthTextMeasurer, LayoutConfig()))
    }
}
