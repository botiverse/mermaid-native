package build.raft.mermaid.core

/**
 * Applies the settings Native implements. Like Mermaid, anything else in `config` (a theme, spacing,
 * another diagram's section) is left alone, so it never stops the diagram from being drawn.
 */
internal object MermaidDocumentOptions {
    fun apply(original: MermaidDiagram, metadata: MermaidFrontmatterMetadata): MermaidDiagram {
        var diagram = original
        val root = metadata.config
        if (root != null) {
            require(root is UsecaseJsonValue.ObjectValue) { "Frontmatter config must be a mapping" }
            for ((section, value) in root.value) {
                if (value !is UsecaseJsonValue.ObjectValue) continue // A scalar such as `theme` or `layout`.
                val fields = value.value
                fun boolean(key: String, fallback: Boolean): Boolean = fields[key]?.let {
                    require(it is UsecaseJsonValue.BooleanValue) { "config.$section.$key must be boolean" }; it.value
                } ?: fallback
                fun string(key: String, fallback: String): String = fields[key]?.let {
                    require(it is UsecaseJsonValue.StringValue) { "config.$section.$key must be a string" }; it.value
                } ?: fallback
                when (section) {
                    "treeView" -> {
                        if (diagram !is TreeViewDiagram) continue
                        fun icons(key: String): Map<String, String> {
                            val objectValue = fields[key] ?: return emptyMap()
                            require(objectValue is UsecaseJsonValue.ObjectValue) { "config.treeView.$key must be a mapping" }
                            return objectValue.value.mapValues { (_, v) -> require(v is UsecaseJsonValue.StringValue) { "Icon references must be strings" }; v.value }
                        }
                        diagram = diagram.copy(iconConfig = TreeViewIconConfig(boolean("showIcons", false), string("defaultIconPack", ""), icons("filenameIcons"), icons("extensionIcons")))
                    }
                    "block" -> {
                        if (diagram !is BlockDiagram) continue
                        val setting = fields["padding"]
                        val padding = if (setting == null) diagram.padding else {
                            require(setting is UsecaseJsonValue.NumberValue && setting.value.isFinite() && setting.value >= 0.0) { "config.block.padding must be a finite non-negative number" }
                            setting.value
                        }
                        diagram = diagram.copy(padding = padding)
                    }
                    "swimlane" -> {
                        if (diagram !is SwimlaneDiagram) continue
                        val setting = fields["lineHops"]
                        val hops = when (setting) {
                            null -> diagram.lineHops
                            is UsecaseJsonValue.BooleanValue -> if (setting.value) SwimlaneLineHops.ARC else SwimlaneLineHops.DISABLED
                            is UsecaseJsonValue.StringValue -> when (setting.value) {
                                "arc" -> SwimlaneLineHops.ARC
                                "gap" -> SwimlaneLineHops.GAP
                                else -> throw IllegalArgumentException("config.swimlane.lineHops must be boolean, 'arc' or 'gap'")
                            }
                            else -> throw IllegalArgumentException("config.swimlane.lineHops must be boolean, 'arc' or 'gap'")
                        }
                        diagram = diagram.copy(lineHops = hops)
                    }
                    "sankey" -> {
                        if (diagram !is SankeyDiagram) continue
                        diagram = diagram.copy(showValues = boolean("showValues", diagram.showValues))
                    }
                    "xyChart" -> {
                        if (diagram !is XyChartDiagram) continue
                        diagram = diagram.copy(showDataLabel = boolean("showDataLabel", diagram.showDataLabel))
                    }
                    "kanban" -> {
                        if (diagram !is KanbanDiagram) continue
                        val template = string("ticketBaseUrl", "")
                        require(template.isEmpty() || isSafeMermaidLink(template)) { "Kanban ticketBaseUrl must be an absolute HTTP(S) URL" }
                        diagram = diagram.copy(ticketBaseUrl = template.takeIf { it.isNotEmpty() })
                    }
                    "treemap" -> {
                        if (diagram !is TreemapDiagram) continue
                        // Another format is drawn as plain numbers.
                        diagram = diagram.copy(valueFormat = string("valueFormat", "").takeIf { it == "," || it == "$0,0" })
                    }
                    "themeVariables" -> {
                        if (diagram !is XyChartDiagram) continue
                        val chart = fields["xyChart"] as? UsecaseJsonValue.ObjectValue ?: continue
                        val palette = chart.value["plotColorPalette"] as? UsecaseJsonValue.StringValue ?: continue
                        val colors = palette.value.split(',').map { it.trim() }
                        // Only hexadecimal colors are supported; any other palette keeps the default one.
                        if (colors.all { Regex("#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})").fullyMatches(it) }) diagram = diagram.copy(plotColorPalette = colors)
                    }
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
