package build.raft.mermaid.core

import kotlin.test.*

class ExampleSyntaxTest {
    @Test fun blockShapesRetainSpansAndReferencedNodeTypes() {
        val d = assertIs<BlockDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("""block-beta
columns 3
user(("User")):3
space:3
ui["Web UI"] db[("Database")]
user --> ui
ui --> db
""")).diagram)
        assertEquals(listOf("circle", "space", "square", "cylinder"), d.nodes.map { it.type })
        assertEquals(3, d.nodes.first().columnSpan)
        assertEquals("Database", d.nodes.last().label)
        assertEquals(listOf(BlockEdge("user", "ui"), BlockEdge("ui", "db")), d.edges)
    }

    @Test fun malformedShapeClosersAndShapedSpacesStillFail() {
        for (node in listOf("A((\"bad\")", "A[(\"bad\"]", "space((\"bad\"))")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("block-beta\n$node"), node)
        }
    }

    @Test fun wardleyNamesAndForcesKeepTheirModelValues() {
        val d = assertIs<WardleyMapDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("""wardley-beta
component byte pair encoding (BPE) [0.53, 0.76]
component Research&Development [0.20, 0.30]
byte pair encoding (BPE) -> Research&Development
accelerator Faster Delivery [0.1, 0.2]
deaccelerator License Play [0.13, 0.78]
deaccelerator "Quoted Force: keep [punctuation]!" [0.4, 0.5]
""")).diagram)
        assertEquals("byte pair encoding (BPE)", d.nodes.first().name)
        assertEquals("Research&Development", d.links.single().to)
        assertEquals(listOf(WardleyNote("Faster Delivery", 0.1, 0.2)), d.accelerators)
        assertEquals(listOf(WardleyNote("License Play", 0.13, 0.78), WardleyNote("Quoted Force: keep [punctuation]!", 0.4, 0.5)), d.deaccelerators)
        for (force in listOf("deaccelerator Bad [0.1, 0.2] junk", "accelerator Bad [1.1, 0.2]")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("wardley-beta\ncomponent A [0.1, 0.2]\n$force"))
        }
    }
}
