package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EntityRelationshipGroupingTest {
    private fun parse(body: String) = assertIs<EntityRelationshipDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("erDiagram\n$body")).diagram)

    @Test fun nestedGroupsPreserveMembershipAndLocalDirections() {
        val diagram = parse("""
            direction LR
            subgraph outer [Outer records]
              A
              subgraph inner
                direction BT
                B ||--o{ C : contains
              end
            end
            A ||--|| D : external
        """.trimIndent())
        assertEquals(FlowDirection.LR, diagram.direction)
        assertEquals(listOf("inner", "outer"), diagram.subgraphs.map { it.id })
        assertEquals(listOf("B", "C"), diagram.subgraphs[0].nodeIds)
        assertEquals(FlowDirection.BT, diagram.subgraphs[0].direction)
        assertEquals(listOf("A", "inner"), diagram.subgraphs[1].nodeIds)
        assertEquals("Outer records", diagram.subgraphs[1].title)
        assertEquals(listOf("outer", "D"), diagram.rootNodeIds)
    }

    @Test fun groupEndpointsDoNotCreateDuplicateEntities() {
        val diagram = parse("subgraph G1\nA\nend\nsubgraph G2\nB\nend\nG1 ||--o{ G2 : connects\nstyle G1 fill:lightblue\nclass G2 group")
        assertEquals(listOf("A", "B"), diagram.entities.map { it.id })
        assertEquals("G1", diagram.relationships.single().from)
        assertEquals("G2", diagram.relationships.single().to)
        assertEquals(listOf("fill:lightblue"), diagram.subgraphs[0].styles)
        assertEquals(listOf("group"), diagram.subgraphs[1].classes)
    }

    @Test fun emptyGroupsAndAllRootDirectionsAreRetained() {
        for (direction in listOf("TB", "BT", "LR", "RL")) {
            val diagram = parse("direction $direction\nsubgraph \"Empty group\"\nend")
            assertEquals(direction, diagram.direction.name)
            assertEquals(emptyList(), diagram.subgraphs.single().nodeIds)
            assertEquals(listOf("Empty group"), diagram.rootNodeIds)
        }
    }

    @Test fun invalidNestingCannotReturnPartialModel() {
        for (body in listOf("end", "subgraph G\nA", "subgraph G\nsubgraph G\nend\nend", "subgraph G\nG\nend", "direction sideways\nA", "subgraph G\nA\nend\nG[alias]", "subgraph G\nA\nend\nG{ int id }", "subgraph G1\nG2\nend\nsubgraph G2\nG1\nend")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("erDiagram\n$body"), body)
        }
    }
}
