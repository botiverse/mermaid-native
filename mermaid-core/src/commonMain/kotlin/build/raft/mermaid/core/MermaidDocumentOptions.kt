package build.raft.mermaid.core

/** Explicit configuration admission: unimplemented settings fail rather than disappear. */
internal object MermaidDocumentOptions {
    fun apply(original: MermaidDiagram, metadata: MermaidFrontmatterMetadata): MermaidDiagram {
        require(metadata.displayMode == null) { "Frontmatter displayMode is not implemented by Native" }
        var diagram = original
        val root = metadata.config
        if (root != null) {
            require(root is UsecaseJsonValue.ObjectValue) { "Frontmatter config must be a mapping" }
            for ((section, value) in root.value) {
                require(value is UsecaseJsonValue.ObjectValue) { "Frontmatter config.$section must be a mapping" }
                val fields = value.value
                fun boolean(key: String, fallback: Boolean): Boolean = fields[key]?.let {
                    require(it is UsecaseJsonValue.BooleanValue) { "config.$section.$key must be boolean" }; it.value
                } ?: fallback
                fun string(key: String, fallback: String): String = fields[key]?.let {
                    require(it is UsecaseJsonValue.StringValue) { "config.$section.$key must be a string" }; it.value
                } ?: fallback
                fun allowed(vararg keys: String) { require(fields.keys.all { it in keys }) { "Unsupported Native frontmatter configuration in $section: ${fields.keys.filter { it !in keys }.joinToString()}" } }
                when (section) {
                    "treeView" -> {
                        allowed("showIcons", "defaultIconPack", "filenameIcons", "extensionIcons")
                        require(diagram is TreeViewDiagram) { "treeView configuration requires a TreeView diagram" }
                        fun icons(key: String): Map<String, String> {
                            val objectValue = fields[key] ?: return emptyMap()
                            require(objectValue is UsecaseJsonValue.ObjectValue) { "config.treeView.$key must be a mapping" }
                            return objectValue.value.mapValues { (_, v) -> require(v is UsecaseJsonValue.StringValue) { "Icon references must be strings" }; v.value }
                        }
                        diagram = diagram.copy(iconConfig = TreeViewIconConfig(boolean("showIcons", false), string("defaultIconPack", ""), icons("filenameIcons"), icons("extensionIcons")))
                    }
                    "sankey" -> {
                        allowed("showValues"); require(diagram is SankeyDiagram) { "sankey configuration requires a Sankey diagram" }
                        diagram = diagram.copy(showValues = boolean("showValues", diagram.showValues))
                    }
                    "xyChart" -> {
                        allowed("showDataLabel"); require(diagram is XyChartDiagram) { "xyChart configuration requires an XY chart" }
                        diagram = diagram.copy(showDataLabel = boolean("showDataLabel", diagram.showDataLabel))
                    }
                    else -> throw IllegalArgumentException("Unsupported Native frontmatter configuration: $section")
                }
            }
        }
        val title = metadata.title ?: return diagram
        return when (val d = diagram) {
            is SequenceDiagram -> d.copy(title = d.title ?: title)
            is PieDiagram -> d.copy(title = d.title ?: title)
            is XyChartDiagram -> d.copy(title = d.title ?: title)
            is GanttDiagram -> d.copy(title = d.title ?: title)
            is TimelineDiagram -> d.copy(title = d.title ?: title)
            is QuadrantChartDiagram -> d.copy(title = d.title ?: title)
            is UserJourneyDiagram -> d.copy(title = d.title ?: title)
            is GitGraphDiagram -> d.copy(title = d.title ?: title)
            is PacketDiagram -> d.copy(title = d.title ?: title)
            is TreemapDiagram -> d.copy(title = d.title ?: title)
            is VennDiagram -> d.copy(title = d.title ?: title)
            is ArchitectureDiagram -> d.copy(title = d.title ?: title)
            is C4Diagram -> d.copy(title = d.title ?: title)
            is CynefinDiagram -> d.copy(title = d.title ?: title)
            is TreeViewDiagram -> d.copy(title = d.title ?: title)
            is RailroadDiagram -> d.copy(title = d.title ?: title)
            is ZenumlDiagram -> d.copy(title = d.title ?: title)
            is RadarChartDiagram -> d.copy(title = d.title ?: title)
            is WardleyMapDiagram -> d.copy(title = d.title ?: title)
            is EventModelingDiagram -> d.copy(title = d.title ?: title)
            else -> diagram // Families without a title property use the document-aware layout overload.
        }
    }
}

/** Declared diagram title, used to avoid painting a second document title. */
public fun MermaidDiagram.declaredTitle(): String? = when (this) {
    is SequenceDiagram -> title
    is PieDiagram -> title
    is XyChartDiagram -> title
    is GanttDiagram -> title
    is TimelineDiagram -> title
    is QuadrantChartDiagram -> title
    is UserJourneyDiagram -> title
    is GitGraphDiagram -> title
    is PacketDiagram -> title
    is TreemapDiagram -> title
    is VennDiagram -> title
    is ArchitectureDiagram -> title
    is C4Diagram -> title
    is CynefinDiagram -> title
    is TreeViewDiagram -> title
    is RailroadDiagram -> title
    is ZenumlDiagram -> title
    is RadarChartDiagram -> title
    is WardleyMapDiagram -> title
    is EventModelingDiagram -> title
    else -> null
}
