package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UsecaseJsonProductionTest {
    @Test
    fun productionRetainsTypedValuesAndSourceOrderWhileGrammarRemainsSyntaxOnly() {
        val source = """usecase-beta
actor User
json Data@{"10":"ten","2":"two","entry":{"old":true},"entry":{"last":3,"first":1}}
User --> Data
"""
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram as UsecaseDiagram
        val data = assertNotNull(result.jsonNodes.single().data)
        assertEquals(listOf("10", "2", "entry"), data.propertyOrder[""])
        assertEquals(listOf("last", "first"), data.propertyOrder["/entry"])
        assertEquals(UsecaseJsonValue.StringValue("two"), data.value["2"])
        val entry = assertIs<UsecaseJsonValue.ObjectValue>(data.value["entry"])
        assertEquals(setOf("last", "first"), entry.value.keys)
        assertTrue(result.jsonNodes.single().source.contains("\"old\":true"))
        assertIs<MermaidParseResult.Success>(UsecaseParser("usecase-beta\njson Broken@{\"key\":,}").parse())
    }

    @Test
    fun productionRejectsInvalidJsonWithDiagramCoordinates() {
        val single = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("usecase-beta\njson Broken@{\"key\":,}"))
        assertEquals(SourceLocation(2, 20), single.diagnostics.single().location)
        assertTrue(single.diagnostics.single().message.contains("Invalid JSON"))
        val multiline = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("usecase-beta\r\njson Broken@{\r\n  \"key\": true,\r\n}"))
        assertEquals(SourceLocation(4, 1), multiline.diagnostics.single().location)
        for (json in listOf("{\"key\":tru}", "{\"key\":01}", "{\"key\":[1,]}", "{\"key\":\"\\q\"}")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("usecase-beta\njson Broken@$json"))
        }
    }
}
