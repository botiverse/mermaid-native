package build.raft.mermaid.core

import kotlin.test.*

class JourneyDocumentTest {
    @Test fun snapshotsStayDetachedAndRepeatedReadsDoNotDuplicateTasks() {
        val d = JourneyDocument(); d.addSection("Trip")
        d.addTask("Drive", ":4:Dad, Mum"); d.addTask("Shop", ":5:Mum")
        val tasks = d.tasks(); val diagram = d.diagram()
        assertEquals(tasks, d.tasks()); assertEquals(listOf("Dad", "Mum"), d.actors())
        d.clear()
        assertTrue(d.tasks().isEmpty()); assertTrue(d.sections().isEmpty()); assertTrue(d.actors().isEmpty())
        assertEquals(2, tasks.size); assertEquals(2, diagram.sections.single().tasks.size)
        assertEquals(listOf("Dad", "Mum"), tasks.first().people)
    }
    @Test fun duplicateAndEmptySectionsKeepInsertionOrderAndIndependentMembership() {
        val d = JourneyDocument(); d.addTask("Before", ":1:User")
        d.addSection("Same"); d.addTask("First", ":2:User")
        d.addSection("Empty"); d.addSection("Same"); d.addTask("Last", ":3:User")
        val diagram = d.diagram()
        assertEquals(listOf("", "Same", "Empty", "Same"), diagram.sections.map { it.name })
        assertEquals(listOf(listOf("Before"), listOf("First"), emptyList(), listOf("Last")), diagram.sections.map { s -> s.tasks.map { it.label } })
    }
    @Test fun invalidScorePreservesPriorStateAndClearResetsMetadata() {
        val d = JourneyDocument(); d.title = "Title"; d.accessibilityTitle = "Accessible"; d.accessibilityDescription = "Description"
        d.addSection("Tasks"); d.addTask("Good", ":5:A")
        assertFailsWith<IllegalArgumentException> { d.addTask("Bad", ":wrong:A") }
        assertEquals(listOf("Good"), d.tasks().map { it.description })
        assertEquals("Accessible", d.diagram().accessibilityTitle)
        d.clear(); assertEquals("", d.title); assertEquals("", d.accessibilityTitle); assertEquals("", d.accessibilityDescription)
        d.addTask("New", ":2:B"); assertEquals("", d.tasks().single().section)
    }
}
