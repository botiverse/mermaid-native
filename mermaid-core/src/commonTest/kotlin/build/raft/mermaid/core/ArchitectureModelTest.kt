package build.raft.mermaid.core

import kotlin.test.*

class ArchitectureModelTest {
    private fun parse(source:String)=assertIs<ArchitectureDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("architecture-beta\n$source")).diagram)
    @Test fun nestedGroupsJunctionsAndHintsSurviveProductionParsing() {
        val d=parse("group root(cloud)[Root]\ngroup child(cloud)[Child] in root\nservice a(server)[A] in child\njunction mid in child\na:B -- T:mid\nalign column a mid")
        assertEquals("root",d.groups[1].parentId)
        assertEquals(ArchitectureJunction("mid","child"),d.junctions.single())
        assertEquals(ArchitectureLayoutHint("column",listOf("a","mid")),d.layoutHints.single())
        assertEquals("mid",d.edges.single().targetId)
    }
    @Test fun groupCyclesAndInvalidJunctionParentsFailAtTheirSourceLine() {
        for(source in listOf("group a in b\ngroup b in a","group a\njunction j in missing","service app\njunction j in app")){
            val failure=assertIs<MermaidParseResult.Failure>(MermaidParser.parse("architecture-beta\n$source"))
            assertTrue(failure.diagnostics.single().location.line>=2)
        }
    }
}
