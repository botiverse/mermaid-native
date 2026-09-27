package build.raft.mermaid.core

/** Sequence has ordered, line-oriented events; comments and metadata retain source locations. */
internal class SequenceParser(private val source: String) {
    private var offset = 0
    private var statementOffset = 0
    private val actors = linkedMapOf<String, SequenceActor>()
    private val events = mutableListOf<SequenceEvent>()
    private val fragments = mutableListOf<SequenceFragmentKind>()
    private val activeDepth = mutableMapOf<String, Int>()
    private var title: String? = null
    private var accTitle: String? = null
    private var accDescription: String? = null

    fun parse(): MermaidParseResult = try {
        skip()
        check(readStatement().equals("sequenceDiagram", true), "Expected sequenceDiagram")
        while (true) {
            skip()
            if (offset == source.length) break
            if (source.startsWith("accDescr", offset, true)) {
                val saved = offset
                offset += 8
                while (source.getOrNull(offset)?.let { it == ' ' || it == '\t' } == true) offset++
                if (source.getOrNull(offset) == '{') {
                    val start = ++offset
                    while (offset < source.length && source[offset] != '}') offset++
                    check(offset < source.length, "Unclosed accessibility description")
                    accDescription = source.substring(start, offset++).trim()
                    continue
                }
                offset = saved
            }
            statement(readStatement())
        }
        check(fragments.isEmpty(), "Unclosed sequence fragment")
        MermaidParseResult.Success(SequenceDiagram(actors.values.toList(), events.filterIsInstance<SequenceMessage>(),
            events.filterIsInstance<SequenceNote>(), events.filterIsInstance<SequenceActivation>(), events.toList(), title, accTitle, accDescription))
    } catch (error: SyntaxError) {
        val prefix = source.take(statementOffset)
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
            error.message ?: "Unsupported sequence syntax", SourceLocation(prefix.count { it == '\n' } + 1, statementOffset - prefix.lastIndexOf('\n')))))
    }

    private fun statement(text: String) {
        if (text.isEmpty()) return
        METADATA.matchEntire(text)?.let {
            if (it.groupValues[1].equals("accTitle", true)) accTitle = it.groupValues[2].trim() else accDescription = it.groupValues[2].trim()
            return
        }
        val command = text.takeWhile { !it.isWhitespace() }.lowercase()
        val rest = text.drop(command.length).trim()
        when (command) {
            "participant", "actor" -> {
                val parts = ALIAS.split(rest, limit = 2)
                val id = parts[0].trim()
                check(validId(id) && '@' !in id, "Invalid participant declaration")
                val label = messageText(parts.getOrElse(1) { id })
                val kind = if (command == "actor") SequenceActorKind.ACTOR else SequenceActorKind.PARTICIPANT
                actors[id] = SequenceActor(id, label.first, kind, label.second)
            }
            "title", "title:" -> title = rest
            "acctitle:" -> accTitle = rest
            "accdescr:" -> accDescription = rest
            "autonumber" -> {
                val values = rest.split(Regex("\\s+")).filter { it.isNotEmpty() }
                check(values.size <= 2, "Invalid autonumber")
                if (values == listOf("off")) events += SequenceNumbering(false)
                else {
                    check(values.all { NUMBER.matches(it) && it.toDoubleOrNull()?.isFinite() == true }, "Sequence numbers allow at most two decimal places")
                    events += SequenceNumbering(true, values.getOrNull(0)?.toDouble(), values.getOrNull(1)?.toDouble() ?: if (values.isEmpty()) null else 1.0)
                }
            }
            "activate", "deactivate" -> activation(rest, command == "activate")
            "note" -> {
                val colon = rest.indexOf(':')
                check(colon >= 0, "Expected : after note participants")
                val declaration = rest.take(colon).trim()
                val position = when {
                    declaration.startsWith("left of ", true) -> SequenceNotePosition.LEFT_OF
                    declaration.startsWith("right of ", true) -> SequenceNotePosition.RIGHT_OF
                    declaration.startsWith("over ", true) -> SequenceNotePosition.OVER
                    else -> fail("Invalid note position")
                }
                val ids = declaration.drop(if (position == SequenceNotePosition.OVER) 5 else if (position == SequenceNotePosition.LEFT_OF) 8 else 9).split(',').map { it.trim() }
                check(ids.size in 1..2 && (position == SequenceNotePosition.OVER || ids.size == 1), "Invalid note participants")
                ids.forEach(::register)
                val label = messageText(rest.substring(colon + 1))
                events += SequenceNote(position, ids, label.first, label.second)
            }
            "loop", "opt", "alt", "par", "par_over", "critical", "break", "rect" -> {
                val kind = SequenceFragmentKind.valueOf(command.uppercase())
                val label = messageText(rest)
                fragments += kind
                events += SequenceFragment(kind, SequenceFragmentBoundary.START, label.first, label.second)
            }
            "else", "and", "option" -> {
                val kind = fragments.lastOrNull() ?: fail("Branch outside sequence fragment")
                check(when (command) { "else" -> kind == SequenceFragmentKind.ALT; "and" -> kind == SequenceFragmentKind.PAR || kind == SequenceFragmentKind.PAR_OVER; else -> kind == SequenceFragmentKind.CRITICAL }, "Invalid sequence fragment branch")
                val label = messageText(rest)
                events += SequenceFragment(kind, SequenceFragmentBoundary.BRANCH, label.first, label.second)
            }
            "end" -> {
                check(rest.isEmpty() && fragments.isNotEmpty(), "Unexpected end")
                events += SequenceFragment(fragments.removeAt(fragments.lastIndex), SequenceFragmentBoundary.END)
            }
            else -> signal(text)
        }
    }

    private fun signal(text: String) {
        val colon = text.indexOf(':')
        check(colon >= 0, "Expected : after sequence message")
        val connection = text.take(colon)
        val arrow = ARROWS.mapNotNull { symbol -> connection.indexOf(symbol).takeIf { it >= 0 }?.let { Triple(symbol, it, symbol.length) } }
            .minWithOrNull(compareBy<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third }) ?: fail("Unsupported sequence arrow")
        var from = connection.take(arrow.second).trim()
        var to = connection.drop(arrow.second + arrow.third).trim()
        val centralFrom = from.endsWith("()")
        val centralTo = to.startsWith("()")
        if (centralFrom) from = from.dropLast(2).trim()
        if (centralTo) to = to.drop(2).trim()
        val activation = to.firstOrNull()?.takeIf { it == '+' || it == '-' }
        if (activation != null) to = to.drop(1).trim()
        register(from); register(to)
        val symbol = arrow.first
        val style = if ("--" in symbol) SequenceLineStyle.DASHED else SequenceLineStyle.SOLID
        val head = when { symbol.endsWith(">>") -> SequenceArrowHead.FILLED; symbol.endsWith('x') -> SequenceArrowHead.CROSS; symbol.endsWith(')') -> SequenceArrowHead.OPEN; else -> SequenceArrowHead.NONE }
        val central = when { centralFrom && centralTo -> SequenceCentralConnection.BOTH; centralFrom -> SequenceCentralConnection.FROM; centralTo -> SequenceCentralConnection.TO; else -> SequenceCentralConnection.NONE }
        val label = messageText(text.substring(colon + 1))
        events += SequenceMessage(from, to, label.first, style, head, label.second, symbol.startsWith("<<"), central, activation == '+' || centralTo)
        if (activation != null) activation(if (activation == '+') to else from, activation == '+')
    }

    private fun activation(id: String, start: Boolean) {
        register(id)
        val depth = activeDepth[id] ?: 0
        check(start || depth > 0, "Trying to inactivate an inactive participant ($id)")
        activeDepth[id] = depth + if (start) 1 else -1
        events += SequenceActivation(id, start)
    }
    private fun register(id: String) {
        check(validId(id), "Invalid sequence participant: $id")
        if (id !in actors) actors[id] = SequenceActor(id, id)
    }
    private fun validId(id: String) = id.isNotBlank() && id.none { it in "<>:,;()\\/+" } && !id.startsWith('-') && !id.endsWith('-')
    private fun messageText(raw: String): Pair<String, Boolean?> {
        val text = raw.trim()
        val match = WRAP.find(text)
        return if (match == null) text to null else text.drop(match.value.length).trim() to !match.value.contains("nowrap")
    }
    private fun skip() {
        while (offset < source.length) {
            if (source[offset].isWhitespace() || source[offset] == ';') offset++
            else if (source[offset] == '#' || source.startsWith("%%", offset)) while (offset < source.length && source[offset] != '\n') offset++
            else break
        }
    }
    private fun readStatement(): String {
        val start = offset
        statementOffset = start
        while (offset < source.length && source[offset] !in "\r\n;#" && !source.startsWith("%%", offset)) offset++
        return source.substring(start, offset).trim()
    }
    private fun check(condition: Boolean, message: String) { if (!condition) fail(message) }
    private fun fail(message: String): Nothing = throw SyntaxError(message)
    private class SyntaxError(message: String) : Exception(message)
    private companion object {
        val METADATA = Regex("(accTitle|accDescr)\\s*:\\s*(.*)", RegexOption.IGNORE_CASE)
        val ALIAS = Regex("\\s+[aA][sS]\\s+")
        val NUMBER = Regex("(?:[0-9]+(?:\\.[0-9]{1,2})?|\\.[0-9]{1,2})")
        val WRAP = Regex("^:?(?:no)?wrap:")
        val ARROWS = listOf("<<-->>", "<<->>", "-->>", "->>", "-->", "->", "--x", "-x", "--)", "-)")
    }
}
