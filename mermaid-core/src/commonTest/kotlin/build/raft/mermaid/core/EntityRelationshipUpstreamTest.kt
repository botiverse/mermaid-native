package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Ports of ER parser assertions from mermaid 04ee3364, er/parser/erDiagram.spec.js. */
class EntityRelationshipUpstreamTest {
    private fun parse(body: String): EntityRelationshipDiagram = assertIs(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("erDiagram\n$body"), body).diagram,
    )

    @Test fun standaloneEntitiesAndEmptyBlocks() {
        for (body in listOf("ISLAND\nMAINLAND", "ISLAND{} MAINLAND {}", "ISLAND\n{}\nMAINLAND { }")) {
            val diagram = parse(body)
            assertEquals(listOf("ISLAND", "MAINLAND"), diagram.entities.map { it.id })
            assertEquals(emptyList(), diagram.relationships)
            assertEquals(listOf(emptyList(), emptyList()), diagram.entities.map { it.attributes })
        }
        assertEquals(listOf("A", "1"), parse("A\n1").entities.map { it.id })
        for (name in listOf("DUCK-BILLED_PLATYPUS", "_foo", "1", "12", "1.5", "u", "__proto__", "constructor", "prototype", "用户")) {
            assertEquals(name, parse(name).entities.single().id)
        }
    }

    @Test fun quotedNamesPreservePunctuationAndSemicolons() {
        for (name in listOf("Token store", "Blo;rf", "Blo:rf", "Blo{rf", "Blo[rf", "Blo#rf", "Blo`rf", "Blo'رف", "Blo€rf")) {
            assertEquals(name, parse("\"$name\"").entities.single().id)
        }
        for (name in listOf("", "Blo%rf", "Blo\\rf", "Blo\nrf", "Blo\rrf", "Blo\brf", "Blo\u000brf")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("erDiagram\n\"$name\""), name)
        }
    }

    @Test fun aliasSurvivesRelationshipsBeforeAndAfterDeclaration() {
        for (body in listOf(
            "foo[\"batman\"]\nfoo ||--o| bar : rel",
            "foo ||--o| bar : rel\nbuzz foo[\"batman\"]",
            "buzz foo[\"batman\"]\nfoo ||--o| bar : rel",
        )) {
            val diagram = parse(body)
            assertEquals("batman", diagram.entities.single { it.id == "foo" }.alias)
            assertEquals(null, diagram.entities.single { it.id == "bar" }.alias)
            assertEquals("foo", diagram.relationships.single().from)
        }
    }

    @Test fun firstAliasWinsAsInUpstreamEntityDatabase() {
        assertEquals("first", parse("A[first] A[second]").entities.single().alias)
    }

    @Test fun attributesCanBeInlineAndSplitAcrossDeclarations() {
        val expected = listOf(EntityAttribute("string", "title"), EntityAttribute("string", "author"), EntityAttribute("float", "price"))
        for (body in listOf(
            "BOOK{string title string author float price}",
            "BOOK {\nstring title\n}\nBOOK { string author\nfloat price }",
        )) assertEquals(expected, parse(body).entities.single().attributes)
        assertEquals("bar", parse("BOOK[bar]{string title}").entities.single().alias)
    }

    @Test fun attributeTypesNamesKeysAndCommentsAreNotDiscarded() {
        val attrs = parse("""
            CUSTOMER {
                int customer_number PK, FK "comment1"
                datetime customer_status_start_datetime PK,UK, FK
                datetime customer_status_end_datetime PK , UK "comment3"
                string customer_firstname
                string customer_lastname "comment5"
            }
        """.trimIndent()).entities.single().attributes
        assertEquals(listOf(EntityKey.PK, EntityKey.FK), listOf(attrs[0].key) + attrs[0].additionalKeys)
        assertEquals(listOf(EntityKey.PK, EntityKey.UK, EntityKey.FK), listOf(attrs[1].key) + attrs[1].additionalKeys)
        assertEquals(listOf(EntityKey.PK, EntityKey.UK), listOf(attrs[2].key) + attrs[2].additionalKeys)
        assertEquals(listOf("comment1", null, "comment3", null, "comment5"), attrs.map { it.comment })
        assertEquals(EntityKey.NONE, attrs[3].key)
        assertEquals(EntityKey.NONE, attrs[4].key)
    }

    @Test fun richAttributeWordsAndNullableTypes() {
        for ((type, name) in listOf(
            "public.geometry(point,4326)" to "location",
            "type~T~" to "type", "string[]" to "readers", "character(10)" to "isbn",
            "varchar(5)" to "postal_code", "string" to "*title",
            "string" to "author-ref[name](1)", "int?" to "age",
        )) {
            assertEquals(EntityAttribute(type, name), parse("BOOK{$type $name}").entities.single().attributes.single())
        }
        assertEquals(EntityAttribute("custom type?", "display name", EntityKey.PK, "description"),
            parse("BOOK{`custom type`? `display name` PK \"description\"}").entities.single().attributes.single())
        for (prefix in "0-[]()") assertIs<MermaidParseResult.Failure>(MermaidParser.parse("erDiagram\nBOOK {string ${prefix}name}"))
    }

    @Test fun cardinalityAliasesPreserveBothEndsAndIdentification() {
        val cards = listOf(
            "||" to EntityCardinality.ONLY_ONE, "only one" to EntityCardinality.ONLY_ONE,
            "one" to EntityCardinality.ONLY_ONE, "1" to EntityCardinality.ONLY_ONE,
            "|o" to EntityCardinality.ZERO_OR_ONE, "o|" to EntityCardinality.ZERO_OR_ONE,
            "one or zero" to EntityCardinality.ZERO_OR_ONE, "zero or one" to EntityCardinality.ZERO_OR_ONE,
            "}|" to EntityCardinality.ONE_OR_MORE, "|{" to EntityCardinality.ONE_OR_MORE,
            "one or more" to EntityCardinality.ONE_OR_MORE, "one or many" to EntityCardinality.ONE_OR_MORE,
            "many(1)" to EntityCardinality.ONE_OR_MORE, "1+" to EntityCardinality.ONE_OR_MORE,
            "}o" to EntityCardinality.ZERO_OR_MORE, "o{" to EntityCardinality.ZERO_OR_MORE,
            "zero or more" to EntityCardinality.ZERO_OR_MORE, "zero or many" to EntityCardinality.ZERO_OR_MORE,
            "many(0)" to EntityCardinality.ZERO_OR_MORE, "many" to EntityCardinality.ZERO_OR_MORE,
            "0+" to EntityCardinality.ZERO_OR_MORE,
        )
        for ((token, card) in cards) for (operator in listOf("--", "..", ".-", "-.", " to ", " optionally to ")) {
            val diagram = parse("A $token$operator$token B : \"has role\"")
            assertEquals(2, diagram.entities.size)
            assertEquals(EntityRelationship("A", "B", card, card, "has role", operator == "--" || operator == " to "), diagram.relationships.single())
        }
    }

    @Test fun recursiveRelationshipsAndNumericNames() {
        assertEquals(1, parse("NODE ||--o{ NODE : \"leads to\"").entities.size)
        for (target in listOf("1", "12", "1.5", "u")) {
            val diagram = parse("A 1--1 $target : has")
            assertEquals(target, diagram.relationships.single().to)
            assertEquals(EntityCardinality.ONLY_ONE, diagram.relationships.single().toCardinality)
        }
        assertEquals("", parse("A ||--|| B : \"\"").relationships.single().label)
        assertEquals("a;b", parse("A ||--|| B : \"a;b\"").relationships.single().label)
    }

    @Test fun styleSeparatorsDoNotConsumeFollowingStatements() {
        val diagram = parse("A\nstyle A fill:red; B")
        assertEquals(listOf("A", "B"), diagram.entities.map { it.id })
        assertEquals(listOf("fill:red"), diagram.entities[0].styles)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("erDiagram\nA\nstyle A font-size:2em"))
    }

    @Test fun stylesClassesAccessibilityAndParentMarker() {
        val diagram = parse("""
            accTitle: graph title
            accDescr { this graph is
                about
                stuff }
            A[Accounts]:::first,second { int id PK }
            A u--o{ B:::second : parent
            style A color:red, stroke: blue
            style A fill:#f9f
            class B first
            classDef first,second fill:#eee, color: pink
        """.trimIndent())
        assertEquals("graph title", diagram.accessibilityTitle)
        assertEquals("this graph is\nabout\nstuff", diagram.accessibilityDescription)
        assertEquals(listOf("color:red", "stroke:blue", "fill:#f9f"), diagram.entities[0].styles)
        assertEquals(listOf("first", "second"), diagram.entities[0].classes)
        assertEquals(listOf("second", "first"), diagram.entities[1].classes)
        assertEquals(mapOf("first" to listOf("fill:#eee", "color:pink"), "second" to listOf("fill:#eee", "color:pink")), diagram.classDefinitions)
        assertEquals(EntityCardinality.MD_PARENT, diagram.relationships.single().fromCardinality)
    }

    @Test fun malformedSyntaxFailsWithoutPartialModel() {
        for (body in listOf("A { string }", "A { string id PK, }", "A { string id", "A ||--|| B", "A ||--|| B :", "A[\"alias\"", "A ||XX|| B : has", "A :::", "direction LR\nA", "A { string id UK\"unterminated }")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("erDiagram\n$body"), body)
        }
    }
}
