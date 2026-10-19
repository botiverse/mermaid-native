package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegexMatchingTest {
    @Test fun wholeMatchBacktracksIntoLaterAlternatives() {
        assertTrue(Regex("#?(?:[0-9a-f]{3}|[0-9a-f]{6})").fullyMatches("123456"))
        assertTrue(Regex("#?(?:[0-9a-f]{3}|[0-9a-f]{6})").fullyMatches("abc"))
        assertFalse(Regex("#(?:[0-9a-f]{3}|[0-9a-f]{6})").fullyMatches("#12345"))
    }
}
