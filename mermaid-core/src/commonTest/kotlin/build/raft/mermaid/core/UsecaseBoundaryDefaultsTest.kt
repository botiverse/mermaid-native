package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UsecaseBoundaryDefaultsTest {
    @Test fun defaultTypePreservesClassesStylesAndExplicitMetadataAcrossDeclarationOrder() {
        val diagram = assertIs<UsecaseDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("""usecase-beta
class Plain highlighted
style Plain stroke:red
systemBoundary Plain
Login
end
systemBoundary Payment@{ type: rect }:::highlighted
Pay
end
Payment@{ type: package }
""")).diagram)
        val plain = diagram.attributes.getValue("Plain")
        assertEquals("rect", plain.properties["type"])
        assertEquals(listOf("highlighted"), plain.classes)
        assertEquals(mapOf("stroke" to "red"), plain.styles)
        assertEquals("package", diagram.attributes.getValue("Payment").properties["type"])
        assertEquals(listOf("highlighted"), diagram.attributes.getValue("Payment").classes)
        assertEquals("Plain", diagram.attributes.getValue("Login").parentId)
        assertEquals("Payment", diagram.attributes.getValue("Pay").parentId)
    }
}
