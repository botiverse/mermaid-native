package build.raft.mermaid.core
import kotlin.test.*
class RequirementGrammarTest {
    private fun parse(source:String)=assertIs<RequirementDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
    @Test fun optionalFieldsPreserveMissingValuesAndAdditionalTypes(){
        val d=parse("requirementDiagram\nphysicalRequirement structure {\n}\ndesignConstraint constraint {\ntext: Keep it light\n}\nelement client {\ntype: application\n}")
        assertEquals(listOf(RequirementType.PHYSICAL_REQUIREMENT,RequirementType.DESIGN_CONSTRAINT),d.requirements.map { it.type });assertEquals(RequirementRisk.UNSPECIFIED,d.requirements[0].risk);assertEquals("",d.elements.single().docRef)
    }
    @Test fun backwardAndUndeclaredRelationshipsRetainAllKinds(){
        val kinds=RequirementRelationshipKind.entries
        val d=parse("requirementDiagram\n"+kinds.joinToString("\n"){ "A <- ${it.name.lowercase()} - B" })
        assertEquals(kinds,d.relationships.map { it.kind });assertTrue(d.relationships.all { it.from=="B"&&it.to=="A" });assertTrue(d.requirements.isEmpty())
    }
    @Test fun classesAndRepeatedStylesRetainBothDeclarations(){
        val d=parse("requirementDiagram\nrequirement r:::hot {\n}\nelement e {\n}\nclassDef hot fill:#dbeafe\nclass r,e large\nclassDef large font-size:24px\nstyle r stroke:#2563eb\nstyle r stroke-width:3px")
        assertEquals(listOf("default","hot","large"),d.requirements.single().classes);assertEquals(listOf("stroke:#2563eb","stroke-width:3px"),d.requirements.single().styles);assertEquals(listOf("font-size:24px"),d.classDefinitions["large"])
    }
    @Test fun quotedNamesDirectionsAndMultilineAccessibilitySurvive(){
        val d=parse("requirementDiagram\naccDescr {\n  First line\nSecond line\n}\ndirection RL\nrequirement \"Light structure\" {\nid: \"R:1\"\ntext: \"literal { braces }\"\n}")
        assertEquals("First line\nSecond line",d.accessibilityDescription);assertEquals(FlowDirection.RL,d.direction);assertEquals("Light structure",d.requirements.single().name);assertEquals("literal { braces }",d.requirements.single().text)
    }
    @Test fun malformedRiskAndUnclosedBlocksFail(){for(s in listOf("requirementDiagram\nrequirement r {\nrisk: extreme\n}","requirementDiagram\nelement e {\ntype: app"))assertIs<MermaidParseResult.Failure>(MermaidParser.parse(s))}
}
