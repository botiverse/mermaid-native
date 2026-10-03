package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UsecaseBoundaryLabelTest {
    private fun boundaries(source: String): List<UsecaseBoundary> =
        assertIs<UsecaseDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram).boundaries

    @Test fun distinguishesMarkdownDelimitersFromLiteralFormattingCharacters() {
        val result = boundaries("""usecase-beta
systemBoundary Markdown["`**Payment** service`"]
end
systemBoundary Plain["**Payment** service"]
end
systemBoundary Bare
end
""")
        assertEquals(listOf("**Payment** service", "**Payment** service", "Bare"), result.map { it.label })
        assertEquals(listOf("markdown", "text", "text"), result.map { it.labelType })
    }

    @Test fun preservesDerivedMarkdownIdsAndMultilineBoundaryLabels() {
        val result = boundaries("""usecase-beta
systemBoundary "`Payment service`"
end
systemBoundary Explicit("`Payment
service`")
end
""")
        assertEquals(listOf("Payment_service", "Explicit"), result.map { it.id })
        assertEquals(listOf("Payment service", "Payment\nservice"), result.map { it.label })
        assertEquals(listOf("markdown", "markdown"), result.map { it.labelType })
    }
}
