package build.raft.mermaid.testkit

import build.raft.mermaid.core.MermaidParser
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Class-init smoke for `MermaidParser.<clinit>`.
 *
 * Every regex the parser uses is constructed eagerly inside the class's static
 * initializer. On Android that initializer runs against the platform ICU regex
 * engine, which accepts a narrower grammar than the JVM Pattern engine — so a
 * pattern that compiles on the JVM can throw `PatternSyntaxException` at
 * `<clinit>` on Android. Because the initializer is all-or-nothing, a single
 * bad pattern poisons the class for the whole process and every later
 * reference fails with `NoClassDefFoundError`, silently degrading every
 * mermaid block to a plain code block on-device.
 *
 * This test asserts nothing about parse output — it only forces the class to
 * initialize under the Android target. If any future pattern introduces a
 * construct ICU cannot compile, this test fails at class-init on the Android
 * unit-test source set even though the JVM common tests stay green.
 */
class MermaidParserClinitTest {
    @Test
    fun parserClassInitializesUnderAndroidRegexEngine() {
        // Touching any member forces <clinit>. A successful call is enough —
        // we do not assert on the result, only that initialization did not
        // throw. (Keep it to a supported subset so the assertion is trivially
        // satisfiable once init succeeds.)
        val result = MermaidParser.parse("graph TD\nA-->B\n")
        assertTrue(
            result is build.raft.mermaid.core.MermaidParseResult,
            "MermaidParser.<clinit> should complete on the Android regex engine",
        )
    }
}
