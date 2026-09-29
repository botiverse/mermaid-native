package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.DiagramPalette

import build.raft.mermaid.core.EntityDefinition
import build.raft.mermaid.core.EntityRelationshipDiagram
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.TextStyle

internal class EntityRelationshipStyle(entity: EntityDefinition, diagram: EntityRelationshipDiagram) {
    private val properties = ((listOf("default") + entity.classes).flatMap { diagram.classDefinitions[it].orEmpty() } + entity.styles)
        .mapNotNull { it.split(':', limit = 2).takeIf { parts -> parts.size == 2 } }
        .associate { it[0].trim().lowercase() to it[1].trim() }
    private fun color(key: String, default: String): SceneColor {
        val value = properties[key]?.lowercase()
        val validHex = value?.startsWith("#") == true && value.length in listOf(4, 5, 7, 9) && value.drop(1).all { it in "0123456789abcdef" }
        return SceneColor(if (validHex) value!! else NAMED_COLORS[value] ?: if (value == "none") "none" else if (value == "transparent") "#00000000" else default)
    }
    private val fontSize = properties["font-size"]?.lowercase()?.removeSuffix("px")?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 && it <= 512.0 }
    private val weight = properties["font-weight"]?.lowercase()?.let { if (it == "bold") 700 else if (it == "normal") 400 else it.toIntOrNull() }?.takeIf { it in 1..1000 }
    val title = TextStyle(fontSize = fontSize ?: 14.0, fontWeight = weight ?: 600, color = color("color", DiagramPalette.INK))
    val name = TextStyle(fontSize = fontSize ?: 14.0, fontWeight = weight ?: 400, color = color("color", DiagramPalette.INK))
    val type = TextStyle(fontSize = fontSize ?: 12.0, fontWeight = weight ?: 400, color = color("color", DiagramPalette.MUTED))
    val key = TextStyle(fontSize = fontSize ?: 11.0, fontWeight = weight ?: 600, color = color("color", DiagramPalette.INK))
    val rowHeight = maxOf(22.0, (fontSize ?: 14.0) + 8.0)
    val headerHeight = maxOf(28.0, (fontSize ?: 14.0) + 14.0)
    val fill = color("fill", DiagramPalette.SURFACE)
    val stroke = color("stroke", DiagramPalette.OUTLINE)
    val strokeWidth = properties["stroke-width"]?.lowercase()?.removeSuffix("px")?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 1.5
    val ruleStrokeWidth = if ("stroke-width" in properties) strokeWidth else 1.0
}

/* CSS named-color values from d3-color 3.1.0.
Copyright 2010-2022 Mike Bostock

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.
*/
internal val NAMED_COLORS = mapOf(
    "aliceblue" to "#f0f8ff",
    "antiquewhite" to "#faebd7",
    "aqua" to "#00ffff",
    "aquamarine" to "#7fffd4",
    "azure" to "#f0ffff",
    "beige" to "#f5f5dc",
    "bisque" to "#ffe4c4",
    "black" to "#000000",
    "blanchedalmond" to "#ffebcd",
    "blue" to "#0000ff",
    "blueviolet" to "#8a2be2",
    "brown" to "#a52a2a",
    "burlywood" to "#deb887",
    "cadetblue" to "#5f9ea0",
    "chartreuse" to "#7fff00",
    "chocolate" to "#d2691e",
    "coral" to "#ff7f50",
    "cornflowerblue" to "#6495ed",
    "cornsilk" to "#fff8dc",
    "crimson" to "#dc143c",
    "cyan" to "#00ffff",
    "darkblue" to "#00008b",
    "darkcyan" to "#008b8b",
    "darkgoldenrod" to "#b8860b",
    "darkgray" to "#a9a9a9",
    "darkgreen" to "#006400",
    "darkgrey" to "#a9a9a9",
    "darkkhaki" to "#bdb76b",
    "darkmagenta" to "#8b008b",
    "darkolivegreen" to "#556b2f",
    "darkorange" to "#ff8c00",
    "darkorchid" to "#9932cc",
    "darkred" to "#8b0000",
    "darksalmon" to "#e9967a",
    "darkseagreen" to "#8fbc8f",
    "darkslateblue" to "#483d8b",
    "darkslategray" to "#2f4f4f",
    "darkslategrey" to "#2f4f4f",
    "darkturquoise" to "#00ced1",
    "darkviolet" to "#9400d3",
    "deeppink" to "#ff1493",
    "deepskyblue" to "#00bfff",
    "dimgray" to "#696969",
    "dimgrey" to "#696969",
    "dodgerblue" to "#1e90ff",
    "firebrick" to "#b22222",
    "floralwhite" to "#fffaf0",
    "forestgreen" to "#228b22",
    "fuchsia" to "#ff00ff",
    "gainsboro" to "#dcdcdc",
    "ghostwhite" to "#f8f8ff",
    "gold" to "#ffd700",
    "goldenrod" to "#daa520",
    "gray" to "#808080",
    "green" to "#008000",
    "greenyellow" to "#adff2f",
    "grey" to "#808080",
    "honeydew" to "#f0fff0",
    "hotpink" to "#ff69b4",
    "indianred" to "#cd5c5c",
    "indigo" to "#4b0082",
    "ivory" to "#fffff0",
    "khaki" to "#f0e68c",
    "lavender" to "#e6e6fa",
    "lavenderblush" to "#fff0f5",
    "lawngreen" to "#7cfc00",
    "lemonchiffon" to "#fffacd",
    "lightblue" to "#add8e6",
    "lightcoral" to "#f08080",
    "lightcyan" to "#e0ffff",
    "lightgoldenrodyellow" to "#fafad2",
    "lightgray" to "#d3d3d3",
    "lightgreen" to "#90ee90",
    "lightgrey" to "#d3d3d3",
    "lightpink" to "#ffb6c1",
    "lightsalmon" to "#ffa07a",
    "lightseagreen" to "#20b2aa",
    "lightskyblue" to "#87cefa",
    "lightslategray" to "#778899",
    "lightslategrey" to "#778899",
    "lightsteelblue" to "#b0c4de",
    "lightyellow" to "#ffffe0",
    "lime" to "#00ff00",
    "limegreen" to "#32cd32",
    "linen" to "#faf0e6",
    "magenta" to "#ff00ff",
    "maroon" to "#800000",
    "mediumaquamarine" to "#66cdaa",
    "mediumblue" to "#0000cd",
    "mediumorchid" to "#ba55d3",
    "mediumpurple" to "#9370db",
    "mediumseagreen" to "#3cb371",
    "mediumslateblue" to "#7b68ee",
    "mediumspringgreen" to "#00fa9a",
    "mediumturquoise" to "#48d1cc",
    "mediumvioletred" to "#c71585",
    "midnightblue" to "#191970",
    "mintcream" to "#f5fffa",
    "mistyrose" to "#ffe4e1",
    "moccasin" to "#ffe4b5",
    "navajowhite" to "#ffdead",
    "navy" to "#000080",
    "oldlace" to "#fdf5e6",
    "olive" to "#808000",
    "olivedrab" to "#6b8e23",
    "orange" to "#ffa500",
    "orangered" to "#ff4500",
    "orchid" to "#da70d6",
    "palegoldenrod" to "#eee8aa",
    "palegreen" to "#98fb98",
    "paleturquoise" to "#afeeee",
    "palevioletred" to "#db7093",
    "papayawhip" to "#ffefd5",
    "peachpuff" to "#ffdab9",
    "peru" to "#cd853f",
    "pink" to "#ffc0cb",
    "plum" to "#dda0dd",
    "powderblue" to "#b0e0e6",
    "purple" to "#800080",
    "rebeccapurple" to "#663399",
    "red" to "#ff0000",
    "rosybrown" to "#bc8f8f",
    "royalblue" to "#4169e1",
    "saddlebrown" to "#8b4513",
    "salmon" to "#fa8072",
    "sandybrown" to "#f4a460",
    "seagreen" to "#2e8b57",
    "seashell" to "#fff5ee",
    "sienna" to "#a0522d",
    "silver" to "#c0c0c0",
    "skyblue" to "#87ceeb",
    "slateblue" to "#6a5acd",
    "slategray" to "#708090",
    "slategrey" to "#708090",
    "snow" to "#fffafa",
    "springgreen" to "#00ff7f",
    "steelblue" to "#4682b4",
    "tan" to "#d2b48c",
    "teal" to "#008080",
    "thistle" to "#d8bfd8",
    "tomato" to "#ff6347",
    "turquoise" to "#40e0d0",
    "violet" to "#ee82ee",
    "wheat" to "#f5deb3",
    "white" to "#ffffff",
    "whitesmoke" to "#f5f5f5",
    "yellow" to "#ffff00",
    "yellowgreen" to "#9acd32",
)
