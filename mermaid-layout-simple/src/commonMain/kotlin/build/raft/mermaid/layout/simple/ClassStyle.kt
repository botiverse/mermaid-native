package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*

internal class ClassStyle(klass:ClassDefinition,diagram:ClassDiagram) {
    private val values=((listOf("default")+klass.classes).flatMap { diagram.classDefinitions[it].orEmpty() }+klass.styles)
        .map { it.split(':',limit=2) }.filter { it.size==2 }.associate { it[0].trim().lowercase() to it[1].trim().lowercase() }
    private fun color(key:String,fallback:String):SceneColor {
        val value=values[key] ?: return SceneColor(fallback)
        val rgb=Regex("rgba?\\(([0-9.]+),\\s*([0-9.]+),\\s*([0-9.]+)(?:,\\s*([0-9.]+))?\\)").matchEntire(value)
        if(rgb!=null) {
            val channels=(1..3).map { rgb.groupValues[it].toDouble().toInt().coerceIn(0,255) }
            val alpha=rgb.groupValues[4].toDoubleOrNull()?.coerceIn(0.0,1.0)
            return SceneColor("#"+channels.joinToString("") { it.toString(16).padStart(2,'0') }+(alpha?.let { (it*255).toInt().toString(16).padStart(2,'0') } ?: ""))
        }
        val hex=value.startsWith('#') && value.length in listOf(4,5,7,9) && value.drop(1).all { it in "0123456789abcdef" }
        return SceneColor(if(hex || value=="none")value else if(value=="transparent")"#00000000" else NAMED_COLORS[value] ?: fallback)
    }
    val fontSize=values["font-size"]?.removeSuffix("px")?.toDoubleOrNull()?.takeIf { it>0 && it<=512 } ?: 14.0
    val text=TextStyle(fontSize=fontSize,fontWeight=values["font-weight"]?.let { if(it=="bold")700 else if(it=="normal")400 else it.toIntOrNull() }?.takeIf { it in 1..1000 } ?: 400,color=color("color","#111827"))
    val lineHeight=maxOf(22.0,fontSize+8)
    val fill=color("fill","#ffffff")
    val stroke=color("stroke","#334155")
    val strokeWidth=values["stroke-width"]?.removeSuffix("px")?.toDoubleOrNull()?.takeIf { it>=0 } ?: 1.5
}
