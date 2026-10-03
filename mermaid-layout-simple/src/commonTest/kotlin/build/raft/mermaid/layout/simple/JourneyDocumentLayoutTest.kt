package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class JourneyDocumentLayoutTest {
    @Test fun editedJourneyUsesTheSameGeometryAndMetadataAsTheRealParser() {
        val doc = JourneyDocument()
        doc.title = "Trip"; doc.accessibilityTitle = "Accessible trip"; doc.accessibilityDescription = "Travel and shop"
        doc.addSection("Travel"); doc.addTask("Drive", ":4:Dad, Mum")
        doc.addSection("Shop"); doc.addTask("Buy", ":5:Mum")
        val source = "journey\ntitle Trip\naccTitle: Accessible trip\naccDescr: Travel and shop\nsection Travel\nDrive:4:Dad, Mum\nsection Shop\nBuy:5:Mum"
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        val actual = SimpleMermaidLayout.layout(doc.diagram(), FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig()), actual)
        val snapshot = doc.diagram(); doc.clear()
        assertEquals(actual, SimpleMermaidLayout.layout(snapshot, FixedWidthTextMeasurer, LayoutConfig()))
        assertTrue(doc.diagram().sections.isEmpty())
    }
}
