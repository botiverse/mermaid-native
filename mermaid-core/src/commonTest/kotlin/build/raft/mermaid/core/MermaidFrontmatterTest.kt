package build.raft.mermaid.core

import kotlin.test.*

class MermaidFrontmatterTest {
    @Test fun originalBodyOffsetsSurviveHeaderRemovalOnEveryNewline() {
        for (newline in listOf("\n", "\r\n", "\r")) {
            val source = listOf("---", "title: Header", "---", "graph SIDEWAYS", "A-->B").joinToString(newline)
            val document = MermaidFrontmatter.extract(source)
            assertEquals(source.indexOf("graph"), document.bodyOffset)
            assertEquals(source.substring(document.bodyOffset), document.text)
            val error = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source)).diagnostics.first()
            assertEquals(SourceLocation(4, 1), error.location)
        }
    }
    @Test fun typedNestedConfigIsPreservedWithoutExecutingStrings() {
        val extracted = MermaidFrontmatter.extract("---\ntitle: true\nconfig:\n  graph:\n    list: [1, false, null, {path: 'a#b'}]\n    words: |\n      first\n      ---\n---\ngraph TD\nA-->B")
        assertEquals("true", extracted.metadata.title)
        val graph = assertIs<UsecaseJsonValue.ObjectValue>(assertIs<UsecaseJsonValue.ObjectValue>(extracted.metadata.config).value.getValue("graph"))
        assertEquals("first\n---\n", assertIs<UsecaseJsonValue.StringValue>(graph.value["words"]).value)
        val list = assertIs<UsecaseJsonValue.ArrayValue>(graph.value["list"]).value
        assertEquals(UsecaseJsonValue.NumberValue(1.0), list[0]); assertEquals(UsecaseJsonValue.BooleanValue(false), list[1]); assertEquals(UsecaseJsonValue.NullValue, list[2])
        assertEquals(UsecaseJsonValue.StringValue("a#b"), assertIs<UsecaseJsonValue.ObjectValue>(list[3]).value["path"])
    }
    @Test fun indentedDelimitersAndLiteralScalarsKeepBodyUnchanged() {
        val source = "\t---\n\ttitle: |\n\t  multi-line\n\t  ---\n\t---\n\tgraph TD\n\tA-->B"
        val result = MermaidFrontmatter.extract(source)
        assertEquals("multi-line\n---\n", result.metadata.title)
        assertEquals("\tgraph TD\n\tA-->B", result.text)
        assertEquals(MermaidDiagramType.FLOWCHART, MermaidParser.detectType(source))
        assertEquals(0, MermaidFrontmatter.extract("---\ntitle: foo\n   ---\ngraph TD").bodyOffset)
    }
    @Test fun malformedAndUnsupportedYamlFailAtTheSourceLocation() {
        for (body in listOf("title: [1", "title: !custom value", "title: *alias", "title: 1\ntitle: 2", "config:\n \tvalue: false")) {
            val result = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\n$body\n---\ngraph TD\nA-->B"))
            assertEquals(MermaidDiagnosticCode.INVALID_VALUE, result.diagnostics.first().code)
            assertTrue(result.diagnostics.first().location.line >= 2)
        }
        val error = assertFailsWith<MermaidFrontmatterError> { MermaidFrontmatter.extract("   ---\n   !!!\n   ---\ndiagram") }
        assertEquals(SourceLocation(2, 4), error.location)
        assertContains(error.message!!, "tag suffix cannot contain exclamation marks")
    }
    @Test fun malformedPlainScalarsAndEmptySequenceSlotsAreNotReinterpreted() {
        for (yaml in listOf("title: foo: bar", "title: [a, , b]", "title: 0xff", "title: .inf")) {
            assertFailsWith<MermaidFrontmatterError>(yaml) { MermaidFrontmatter.extract("---\n$yaml\n---\ndiagram") }
        }
        assertEquals("foo: bar", MermaidFrontmatter.extract("---\ntitle: 'foo: bar'\n---\ndiagram").metadata.title)
        assertEquals("0xff", MermaidFrontmatter.extract("---\ntitle: '0xff'\n---\ndiagram").metadata.title)
    }
    @Test fun limitsRejectExcessiveInputAndRecursiveCollections() {
        assertFailsWith<MermaidFrontmatterError> { MermaidFrontmatter.extract("---\ntitle: " + "x".repeat(262145) + "\n---\ngraph TD") }
        assertFailsWith<MermaidFrontmatterError> { MermaidFrontmatter.extract("---\nconfig: " + "[".repeat(70) + "0" + "]".repeat(70) + "\n---\ngraph TD") }
    }
    @Test fun bodyTitleWinsAndConfigurationIsLocalToOneParse() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("---\ntitle: Header\nconfig:\n  xyChart:\n    showDataLabel: false\n---\nxychart-beta\ntitle \"Body\"\nx-axis [A, B]\nbar [1, 2]"))
        val diagram = assertIs<XyChartDiagram>(parsed.diagram)
        assertEquals("Body", diagram.title); assertFalse(diagram.showDataLabel)
        assertEquals("Header", parsed.frontmatter.title)
        val plain = assertIs<MermaidParseResult.Success>(MermaidParser.parse("xychart-beta\nx-axis [A,B]\nbar [1,2]"))
        assertTrue(assertIs<XyChartDiagram>(plain.diagram).showDataLabel)
        assertNull(plain.frontmatter.config)
    }
    @Test fun unsupportedConfigurationDoesNotSilentlyPass() {
        val result = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\nconfig:\n  unknown: {enabled: true}\n---\ngraph TD\nA-->B"))
        assertContains(result.diagnostics.single().message, "Unsupported Native frontmatter configuration")
        val typed = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\nconfig:\n  sankey:\n    showValues: 'false'\n---\nsankey\nA,B,1"))
        assertContains(typed.diagnostics.single().message, "must be boolean")
    }
    @Test fun treeIconsReachTheExistingProductionSelectionPolicy() {
        val doc = assertIs<MermaidParseResult.Success>(MermaidParser.parse("---\nconfig:\n  treeView:\n    showIcons: true\n    defaultIconPack: custom\n    extensionIcons: {'.ts': typescript}\n---\ntreeView-beta\nroot/\n  app.ts"))
        val tree = assertIs<TreeViewDiagram>(doc.diagram)
        assertEquals("custom:typescript", TreeViewIcons.getNodeIcon(tree.nodes.last(), tree.iconConfig))
    }
}
