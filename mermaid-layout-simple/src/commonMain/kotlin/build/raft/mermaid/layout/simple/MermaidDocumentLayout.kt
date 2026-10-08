package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.declaredTitle
import build.raft.mermaid.layout.*

/** Render a parsed document, retaining frontmatter titles for families without a title field. */
public fun SimpleMermaidLayout.layout(
    parsed: MermaidParseResult.Success,
    textMeasurer: TextMeasurer,
    config: LayoutConfig,
): LayoutScene {
    val scene = layout(parsed.diagram, textMeasurer, config)
    val title = parsed.frontmatter.title ?: return scene
    if (parsed.diagram.declaredTitle() != null) return scene
    val style = TextStyle(fontSize = 18.0, fontWeight = 600)
    val lines = title.trimEnd('\n').split('\n')
    val lineHeight = textMeasurer.measure("M", style).height
    val dy = lineHeight * lines.size + config.padding
    fun ScenePoint.move(): ScenePoint = copy(y = y + dy)
    fun SceneRect.move(): SceneRect = copy(y = y + dy)
    val commands = scene.commands.map { command -> when (command) {
        is DrawRect -> command.copy(rect = command.rect.move())
        is DrawEllipse -> command.copy(center = command.center.move())
        is DrawLine -> command.copy(from = command.from.move(), to = command.to.move())
        is DrawPolyline -> command.copy(points = command.points.map { it.move() })
        is DrawPath -> command.translated(0.0, dy)
        is DrawPolygon -> command.copy(points = command.points.map { it.move() }, gradient = command.gradient?.let { it.copy(from = it.from.move(), to = it.to.move()) })
        is DrawText -> command.copy(origin = command.origin.move())
    } }
    val width = maxOf(scene.width, lines.maxOf { textMeasurer.measure(it, style).width } + 2 * config.padding)
    return scene.copy(width = width, height = scene.height + dy,
        commands = lines.mapIndexed { i, line -> DrawText(line, ScenePoint(width / 2, config.padding + lineHeight * (i + 0.8)), TextAnchor.MIDDLE, style) } + commands,
        links = scene.links.map { it.copy(rect = it.rect.move()) },
        accessibilityTitle = scene.accessibilityTitle ?: title,
        layoutValidation = scene.layoutValidation?.let { report ->
            val geometry = report.geometry.copy(
                nodes = report.geometry.nodes.map { it.copy(bounds = it.bounds.move(), groupTitleBounds = it.groupTitleBounds?.move()) },
                edges = report.geometry.edges.map { it.copy(points = it.points.map { p -> p.move() }, labelBounds = it.labelBounds?.move()) },
            )
            report.copy(geometry = geometry, result = OrthogonalLayoutValidator.validate(geometry), quality = report.quality?.let { LayoutQualityScorer.score(geometry) })
        },
    )
}
