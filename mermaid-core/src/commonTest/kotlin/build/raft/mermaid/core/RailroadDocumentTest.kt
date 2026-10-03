package build.raft.mermaid.core

import kotlin.test.*

class RailroadDocumentTest {
    @Test fun duplicatesPreserveOrderWhileLookupUsesLastDefinitionAndClearResetsAllState() {
        val doc = RailroadDocument()
        doc.title = "Title"; doc.accessibilityTitle = "  Accessible"; doc.accessibilityDescription = "A\n  B"
        doc.addRule(RailroadRule("rule", RailroadTerminal("first")))
        doc.addRule(RailroadRule("rule", RailroadTerminal("second")))
        assertEquals(listOf("first", "second"), doc.rules().map { (it.definition as RailroadTerminal).label })
        assertEquals("second", (doc.rule("rule")!!.definition as RailroadTerminal).label)
        assertNull(doc.rule("missing"))
        val old = doc.diagram()
        assertEquals("Accessible", old.accTitle); assertEquals("A\nB", old.accDescription)
        doc.clear()
        assertTrue(doc.rules().isEmpty()); assertNull(doc.rule("rule"))
        assertEquals("", doc.title); assertEquals("", doc.accessibilityTitle); assertEquals("", doc.accessibilityDescription)
        assertEquals(2, old.rules.size)
    }

    @Test fun nestedTextCleanupAndSnapshotsDoNotMutateCallerOwnedExpressions() {
        val doc = RailroadDocument()
        val children = mutableListOf<RailroadNode>(
            RailroadTerminal("value<SCRIPT type='text/javascript'>\nalert(1)\n</SCRIPT>"),
            RailroadChoice(listOf(RailroadNonTerminal("name<style>x</style>"), RailroadSpecial("hint<script>unfinished"))),
        )
        val input = RailroadRule("rule<script>alert(1)</script>", RailroadSequence(children))
        doc.addRule(input)
        children.clear()
        val stored = assertIs<RailroadSequence>(doc.rule("rule")!!.definition)
        assertEquals("value", assertIs<RailroadTerminal>(stored.children[0]).label)
        val alternatives = assertIs<RailroadChoice>(stored.children[1]).children
        assertEquals("name", assertIs<RailroadNonTerminal>(alternatives[0]).label)
        assertEquals("hint", assertIs<RailroadSpecial>(alternatives[1]).text)
        doc.title = "Safe <script>alert(1)</script>Title"
        assertEquals("Safe Title", doc.title)
        doc.addRule(RailroadRule("literal", RailroadTerminal("a < b & c > d")))
        assertEquals("a < b & c > d", assertIs<RailroadTerminal>(doc.rule("literal")!!.definition).label)
    }
}
