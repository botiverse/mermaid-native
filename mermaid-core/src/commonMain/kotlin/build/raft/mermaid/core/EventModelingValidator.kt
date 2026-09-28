package build.raft.mermaid.core

/** Source-kind rules shared by parsed diagrams and direct typed validation. */
public object EventModelingValidator {
    public fun errors(target: String, sources: List<String>): List<String> {
        val normalized = normalize(target)
        val expected = when (normalized) {
            "command" -> listOf("ui", "processor")
            "event" -> listOf("command")
            "read model" -> listOf("event")
            "processor", "ui" -> listOf("read model")
            else -> return emptyList()
        }
        return sources.filter { normalize(it) !in expected }.map {
            "A $normalized can only receive input from a ${expected.joinToString(" or ")}, not from '$it'."
        }
    }
    private fun normalize(value: String): String = when (value) {
        "cmd", "command" -> "command"
        "evt", "event" -> "event"
        "rmo", "readmodel" -> "read model"
        "pcr", "processor" -> "processor"
        else -> value
    }
}
