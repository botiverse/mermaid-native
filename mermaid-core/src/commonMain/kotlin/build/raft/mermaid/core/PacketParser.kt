package build.raft.mermaid.core

/** Original packet field syntax with resolved contiguous bit ranges. */
internal class PacketParser(private val source: String) {
    fun parse(): MermaidParseResult {
        var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
        val fields = mutableListOf<PacketField>()
        var previousEnd = -1; var headerSeen = false; var description: StringBuilder? = null
        fun failure(message: String, line: Int): MermaidParseResult.Failure = MermaidParseResult.Failure(
            listOf(MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, message, SourceLocation(line, 1))),
        )
        source.lines().forEachIndexed { index, raw ->
            val line = index + 1
            var text = raw.trim()
            if (description != null) {
                val end = text.indexOf('}')
                if (end < 0) { description!!.append('\n').append(text); return@forEachIndexed }
                description!!.append('\n').append(text.substring(0, end)); accDescription = description.toString().trim(); description = null
                text = text.substring(end + 1).trim()
            }
            if (text.isEmpty() || text.startsWith("%%")) return@forEachIndexed
            if (!headerSeen) {
                if (text != "packet" && text != "packet-beta") return failure("Expected packet or packet-beta", line)
                headerSeen = true; return@forEachIndexed
            }
            when {
                text.startsWith("title") -> title = text.substring(5).substringBefore("%%").trim()
                Regex("^accTitle\\s*:").containsMatchIn(text) -> accTitle = text.substringAfter(':').substringBefore("%%").trim()
                Regex("^accDescr\\s*:").containsMatchIn(text) -> accDescription = text.substringAfter(':').substringBefore("%%").trim()
                Regex("^accDescr\\s*\\{").containsMatchIn(text) -> {
                    val body = text.substringAfter('{')
                    if ('}' in body) accDescription = body.substringBefore('}').trim() else description = StringBuilder(body)
                }
                else -> {
                    val match = Regex("^(?:([0-9]+)(?:\\s*-\\s*([0-9]+))?|\\+([0-9]+))\\s*:\\s*([\"'])(.*?)\\4\\s*(?:%%.*)?$").matchEntire(text)
                        ?: return failure("Unsupported packet field syntax", line)
                    val rawStart = match.groupValues[1]; val rawEnd = match.groupValues[2]; val rawBits = match.groupValues[3]
                    val start = if (rawStart.isEmpty()) previousEnd + 1 else rawStart.toIntOrNull() ?: return failure("Packet bit index is too large", line)
                    val givenEnd = if (rawEnd.isEmpty()) null else rawEnd.toIntOrNull() ?: return failure("Packet bit index is too large", line)
                    if (givenEnd != null && givenEnd < start) return failure("Packet block $start - $givenEnd is invalid. End must be greater than start.", line)
                    if (start != previousEnd + 1) return failure("Packet block $start - ${givenEnd ?: start} is not contiguous. It should start from ${previousEnd + 1}.", line)
                    val bits = if (rawBits.isEmpty()) null else rawBits.toIntOrNull() ?: return failure("Packet bit count is too large", line)
                    if (bits == 0) return failure("Packet block $start is invalid. Cannot have a zero bit field.", line)
                    val end = givenEnd?.toLong() ?: (start.toLong() + (bits ?: 1) - 1)
                    if (end > 4095) return failure("Packet layout supports bit indices up to 4095", line)
                    fields += PacketField(start, end.toInt(), match.groupValues[5])
                    previousEnd = end.toInt()
                }
            }
        }
        if (!headerSeen) return failure("Expected packet or packet-beta", 1)
        if (description != null) return failure("Unclosed packet accessibility description", source.lines().size)
        return MermaidParseResult.Success(PacketDiagram(title, fields, accTitle, accDescription))
    }
}
