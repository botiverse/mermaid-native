package build.raft.mermaid.core

import kotlin.test.*

class CynefinDocumentTest {
    @Test fun updatesMergeDomainsReplaceTransitionsAndKeepSnapshotsDetached() {
        val doc = CynefinDocument()
        val items = mutableListOf("First")
        doc.setDomains(listOf(CynefinDomainBlock(CynefinDomain.COMPLEX, items)))
        items += "External mutation"
        doc.setDomains(listOf(CynefinDomainBlock(CynefinDomain.CLEAR, emptyList())))
        doc.setTransitions(listOf(CynefinTransition(CynefinDomain.COMPLEX, CynefinDomain.CLEAR, ""), CynefinTransition(CynefinDomain.CLEAR, CynefinDomain.CLEAR)))
        val snapshot = doc.diagram()
        assertEquals(listOf("First"), snapshot.domains.first().items)
        assertEquals(2, snapshot.domains.size); assertEquals(1, snapshot.transitions.size); assertNull(snapshot.transitions.first().label)
        doc.setDomains(null); doc.setTransitions(null)
        assertEquals(snapshot, doc.diagram())
        doc.setDomains(listOf(CynefinDomainBlock(CynefinDomain.COMPLEX, listOf("Replacement"))))
        doc.setTransitions(emptyList())
        assertEquals(listOf("Replacement"), doc.domains().first().items); assertTrue(doc.transitions().isEmpty())
        doc.title = "Title"; doc.accessibilityTitle = "Accessible"; doc.accessibilityDescription = "Description"
        doc.clear()
        assertTrue(doc.domains().isEmpty()); assertTrue(doc.transitions().isEmpty())
        assertEquals("", doc.title); assertEquals("", doc.accessibilityTitle); assertEquals("", doc.accessibilityDescription)
        assertEquals(2, snapshot.domains.size)
    }
}
