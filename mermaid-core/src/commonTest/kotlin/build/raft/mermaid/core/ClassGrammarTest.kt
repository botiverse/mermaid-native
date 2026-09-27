package build.raft.mermaid.core
import kotlin.test.*

class ClassGrammarTest {
    private fun parse(source:String)=assertIs<ClassDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("classDiagram\n$source")).diagram)
    @Test fun rawBodiesKeepUnmarkedMembersGenericsAndSemicolons() {
        val d=parse("class `Order list`~T~ {\nitems List~T~\n+get(key: String) T\ntext;still one member\n}")
        val c=d.classes.single()
        assertEquals("Order list",c.id);assertEquals("T",c.genericType)
        assertEquals(3,c.members.size);assertFalse(c.members[0].hasVisibility);assertTrue(c.members[1].hasVisibility)
        assertEquals("text;still one member",c.members[2].signature)
    }
    @Test fun nestedNamespacesKeepExplicitHierarchyAndLabels() {
        val d=parse("namespace Company.Engineering[\"Engineering team\"] {\nnamespace Backend { class Service }\nclass Client\n}\nService --> Client")
        assertEquals(listOf("Company","Company.Engineering","Company.Engineering.Backend"),d.namespaces.map { it.id })
        assertFalse(d.namespaces.first().explicit)
        assertEquals("Engineering team",d.namespaces[1].label)
        assertEquals("Company.Engineering.Backend",d.classes.first().namespaceName)
        assertEquals("Company.Engineering",d.classes.last().namespaceName)
    }
    @Test fun bothRelationshipEndsAndDashesRemainExplicit() {
        val d=parse("A \"one\" o..|> \"many\" B : stores\nB --* C")
        val r=d.relationships.first()
        assertEquals(ClassMarker.AGGREGATION,r.fromMarker);assertEquals(ClassMarker.INHERITANCE,r.toMarker);assertEquals(true,r.dashed)
        assertEquals("one",r.fromCardinality);assertEquals("many",r.toCardinality)
        assertEquals(ClassMarker.COMPOSITION,d.relationships.last().toMarker)
    }
    @Test fun labelsAnnotationsDirectionAndAccessibilityAreTyped() {
        val d=parse("direction LR\naccTitle: Class #1; keep\naccDescr { description\nsecond }\nclass A[\"Pretty\"] <<interface>> {\n+run()\n}\n<<service>> A")
        assertEquals(FlowDirection.LR,d.direction);assertEquals("Class #1; keep",d.accessibilityTitle)
        assertEquals("description\nsecond",d.accessibilityDescription)
        assertEquals("Pretty",d.classes.single().label);assertEquals(listOf("interface","service"),d.classes.single().annotations)
    }
    @Test fun quotedNotesRetainKeywordsAndNamespaceMembership() {
        val d=parse("namespace Team {\nclass A\nnote for A \"class {} : value; still one note\"\n}\nnote \"two\nlines\"")
        assertEquals(ClassNote("class {} : value; still one note","A","Team"),d.notes.first())
        assertEquals("two\nlines",d.notes.last().text)
    }
    @Test fun malformedBodiesAndUnsupportedDirectivesFail() {
        listOf("class A {", "namespace A { class B", "class A { member { nested } }", "style A fill:red").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("classDiagram\n$it"),it)
        }
    }
}
