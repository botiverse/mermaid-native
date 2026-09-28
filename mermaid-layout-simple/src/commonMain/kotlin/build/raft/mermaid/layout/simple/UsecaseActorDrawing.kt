package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*

/** Actor metadata is resolved by the production parser, never inferred by a host renderer. */
internal fun drawUsecaseActor(actor: UsecaseActor, p: ScenePoint, fill: SceneColor, stroke: SceneColor): List<DrawCommand> = buildList {
    fun point(x: Double, y: Double) = ScenePoint(p.x + x, p.y + y)
    when (actor.type) {
        UsecaseActorType.NORMAL -> {
            add(DrawEllipse(point(0.0, -20.0), 10.0, 10.0, fill = SceneColor("#ffffff"), stroke = stroke))
            add(DrawLine(point(0.0, -10.0), point(0.0, 20.0), stroke = stroke))
            add(DrawLine(point(-15.0, 0.0), point(15.0, 0.0), stroke = stroke))
            add(DrawLine(point(0.0, 20.0), point(-13.0, 38.0), stroke = stroke))
            add(DrawLine(point(0.0, 20.0), point(13.0, 38.0), stroke = stroke))
        }
        UsecaseActorType.HOLLOW -> {
            add(DrawEllipse(point(0.0, -23.0), 9.0, 9.0, fillOpacity = 0.0, stroke = stroke))
            val outline = listOf(-22.0 to -10.0, 22.0 to -10.0, 22.0 to 0.0, 6.0 to 0.0, 22.0 to 17.0, 13.0 to 28.0, 0.0 to 13.0, -13.0 to 28.0, -22.0 to 17.0, -6.0 to 0.0, -22.0 to 0.0, -22.0 to -10.0)
            add(DrawPolyline(outline.map { point(it.first, it.second) }, stroke = stroke))
        }
        UsecaseActorType.AWESOME -> {
            add(DrawEllipse(point(0.0, -21.0), 13.0, 13.0, fill = stroke, stroke = stroke))
            add(DrawRect(SceneRect(p.x - 24, p.y - 3, 48.0, 34.0), 16.0, fill = stroke, stroke = stroke))
        }
        UsecaseActorType.ICON -> {
            // Native does not load arbitrary SVG icon packs. Preserve a visible, deterministic fallback.
            add(DrawRect(SceneRect(p.x - 26, p.y - 28, 52.0, 52.0), 4.0, fill = fill, stroke = stroke))
            add(DrawText("?", point(0.0, 7.0), TextAnchor.MIDDLE, TextStyle(fontSize = 28.0, color = stroke)))
        }
    }
    if (actor.business) {
        val cy = if (actor.type == UsecaseActorType.HOLLOW) -23.0 else -20.0
        val radius = if (actor.type == UsecaseActorType.HOLLOW) 9.0 else 10.0
        // A right-side chord, sharing the actor stroke, remains inside the head outline.
        val offset = radius * .7071067811865476
        add(DrawLine(point(0.0, cy - radius), point(offset, cy + offset), stroke = stroke))
    }
}
