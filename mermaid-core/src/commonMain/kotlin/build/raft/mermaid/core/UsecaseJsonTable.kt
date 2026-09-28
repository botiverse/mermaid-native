package build.raft.mermaid.core

/** Renderer-ready plain text. Hosts must render cells as text, never interpret them as markup. */
public data class UsecaseJsonRow(val key: String, val accessibleKey: String, val value: String)

public object UsecaseJsonTable {
    public fun rows(data: UsecaseOrderedJsonObject): List<UsecaseJsonRow> = buildList {
        fun scalar(value: UsecaseJsonValue): String = when (value) {
            is UsecaseJsonValue.StringValue -> value.value
            is UsecaseJsonValue.BooleanValue -> value.value.toString()
            is UsecaseJsonValue.NumberValue -> value.value.let { n ->
                if (n == 0.0) "0" else if (n >= Long.MIN_VALUE.toDouble() && n < Long.MAX_VALUE.toDouble() && n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
            }
            UsecaseJsonValue.NullValue -> "null"
            else -> error("Expected scalar JSON value")
        }
        fun visit(value: UsecaseJsonValue, path: String, pointer: String) {
            when (value) {
                is UsecaseJsonValue.ArrayValue -> {
                    if (value.value.isEmpty()) add(UsecaseJsonRow(path, path, "[]"))
                    else if (value.value.all { it !is UsecaseJsonValue.ArrayValue && it !is UsecaseJsonValue.ObjectValue }) {
                        value.value.forEachIndexed { i, item -> add(UsecaseJsonRow(if (i == 0) path else "", path, scalar(item))) }
                    } else value.value.forEachIndexed { i, item -> visit(item, "$path[$i]", "$pointer/$i") }
                }
                is UsecaseJsonValue.ObjectValue -> {
                    val keys = data.propertyOrder[pointer] ?: value.value.keys.toList()
                    if (keys.isEmpty()) add(UsecaseJsonRow(path, path, "{}"))
                    for (key in keys) {
                        val item = value.value[key] ?: continue
                        visit(item, if (path.isEmpty()) key else "$path.$key", "$pointer/${key.replace("~", "~0").replace("/", "~1")}")
                    }
                }
                else -> add(UsecaseJsonRow(path, path, scalar(value)))
            }
        }
        visit(UsecaseJsonValue.ObjectValue(data.value), "", "")
    }
}
