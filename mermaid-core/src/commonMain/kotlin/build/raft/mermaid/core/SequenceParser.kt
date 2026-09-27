package build.raft.mermaid.core

/** Sequence has ordered, line-oriented events; comments and metadata retain source locations. */
internal class SequenceParser(private val source: String) {
    private var offset = 0
    private var statementOffset = 0
    private val actors = linkedMapOf<String, SequenceActor>()
    private val events = mutableListOf<SequenceEvent>()
    private val fragments = mutableListOf<SequenceFragmentKind>()
    private val boxes = mutableListOf<SequenceBox>()
    private var box: SequenceBox? = null
    private var pendingLifecycle: SequenceLifecycle? = null
    private val destroyed = mutableSetOf<String>()
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
        check(box == null, "Unclosed participant box")
        check(pendingLifecycle == null, "Lifecycle directive must be followed by a message")
        MermaidParseResult.Success(SequenceDiagram(actors.values.toList(), events.filterIsInstance<SequenceMessage>(),
            events.filterIsInstance<SequenceNote>(), events.filterIsInstance<SequenceActivation>(), events.toList(), title, accTitle, accDescription, boxes.toList()))
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
        check(box == null || command in listOf("participant", "actor", "end"), "Participant boxes only contain declarations")
        when (command) {
            "box" -> {
                check(box == null && fragments.isEmpty(), "Invalid nested participant box")
                val match = BOX_COLOR.find(rest)
                val token = match?.value ?: rest.substringBefore(' ')
                val hasColor = token.lowercase() in SEQUENCE_COLOR_NAMES || token.startsWith("rgb") || token.startsWith('#')
                box = SequenceBox(if (hasColor) rest.drop(token.length).trim() else rest, if (hasColor) token else "transparent", emptyList())
            }
            "links", "link", "properties" -> actorData(command, rest)
            "create" -> {
                check(pendingLifecycle == null, "Lifecycle directive must be followed by a message")
                val kind = rest.substringBefore(' ').lowercase()
                check(kind == "participant" || kind == "actor", "Expected create participant or actor")
                val before = actors.keys.toSet()
                participant(rest.substringAfter(' '), kind == "actor")
                val created = actors.keys - before
                check(created.size == 1, "Cannot create an existing participant")
                val event = SequenceLifecycle(created.single(), true)
                events += event; pendingLifecycle = event
            }
            "destroy" -> {
                check(pendingLifecycle == null && rest in actors && rest !in destroyed, "Invalid participant destruction")
                val event = SequenceLifecycle(rest, false)
                events += event; pendingLifecycle = event
            }
            "participant", "actor" -> participant(rest, command == "actor")
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
                box?.let { boxes += it; box = null; return }
                check(rest.isEmpty() && fragments.isNotEmpty(), "Unexpected end")
                events += SequenceFragment(fragments.removeAt(fragments.lastIndex), SequenceFragmentBoundary.END)
            }
            else -> signal(text)
        }
    }

    private fun participant(raw: String, actor: Boolean) {
        val marker = raw.indexOf("@{")
        val metadata: Map<String,String>
        val declaration: String
        if (marker >= 0) {
            var at=marker+2; var quote: Char?=null; var escaped=false
            while(at<raw.length) {
                val c=raw[at]
                if(escaped) escaped=false
                else if(c=='\\' && quote!=null) escaped=true
                else if(quote!=null) { if(c==quote) quote=null }
                else if(c=='\'' || c=='"') quote=c
                else if(c=='}') break
                at++
            }
            check(at<raw.length, "Unclosed participant metadata")
            metadata=SequenceParticipantMetadata(raw.substring(marker+2,at)).parse() ?: fail("Invalid participant metadata")
            declaration=raw.take(marker).trim()+raw.substring(at+1)
        } else { metadata=emptyMap(); declaration=raw }
        val parts=ALIAS.split(declaration,limit=2)
        val id=parts[0].trim()
        check(validId(id) && '@' !in id,"Invalid participant declaration")
        check(metadata.keys.all { it=="type" || it=="alias" }, "Unsupported participant metadata field")
        val kind=metadata["type"]?.let { type -> SequenceActorKind.entries.firstOrNull { it.name.equals(type,true) } ?: fail("Unsupported participant type: $type") }
            ?: if(actor) SequenceActorKind.ACTOR else SequenceActorKind.PARTICIPANT
        val external=parts.getOrNull(1)
        val rawLabel=if(external==null || external==id) metadata["alias"] ?: external else external
        box?.let { current ->
            check(boxes.none { id in it.actorIds }, "Participant cannot belong to multiple boxes")
            box = current.copy(actorIds = (current.actorIds + id).distinct())
        }
        if(rawLabel==null && id in actors) return
        val label=messageText(rawLabel ?: id)
        actors[id]=SequenceActor(id,label.first,kind,label.second,actors[id]?.links.orEmpty(),actors[id]?.properties.orEmpty())
    }

    private fun actorData(command: String, rest: String) {
        val colon = rest.indexOf(':')
        check(colon > 0, "Expected participant data")
        val id = rest.take(colon).trim()
        val actor = actors[id] ?: fail("Unknown participant: $id")
        val data = rest.drop(colon + 1).trim()
        val entries = if (command == "link") {
            val at = data.indexOf('@')
            check(at > 0 && at < data.lastIndex, "Expected link label @ URL")
            mapOf(data.take(at).trim() to data.drop(at + 1).trim())
        } else {
            check(data.startsWith('{') && data.endsWith('}'), "Expected participant data object")
            SequenceParticipantMetadata(data.drop(1).dropLast(1)).parse() ?: fail("Invalid participant data object")
        }
        actors[id] = if (command == "properties") actor.copy(properties = actor.properties + entries)
            else actor.copy(links = actor.links + entries)
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
        check(from !in destroyed && to !in destroyed, "Message references a destroyed participant")
        pendingLifecycle?.let { lifecycle ->
            check(if (lifecycle.create) to == lifecycle.actorId else from == lifecycle.actorId || to == lifecycle.actorId,
                "Lifecycle directive must target the next message")
            if (!lifecycle.create) destroyed += lifecycle.actorId
            pendingLifecycle = null
        }
        register(from); register(to)
        val symbol = arrow.first
        val style = if ("--" in symbol) SequenceLineStyle.DASHED else SequenceLineStyle.SOLID
        val half = HALF_ARROWS[symbol]
        val head = half?.first ?: when { symbol.endsWith(">>") -> SequenceArrowHead.FILLED; symbol.endsWith('x') -> SequenceArrowHead.CROSS; symbol.endsWith(')') -> SequenceArrowHead.OPEN; else -> SequenceArrowHead.NONE }
        val central = when { centralFrom && centralTo -> SequenceCentralConnection.BOTH; centralFrom -> SequenceCentralConnection.FROM; centralTo -> SequenceCentralConnection.TO; else -> SequenceCentralConnection.NONE }
        val label = messageText(text.substring(colon + 1))
        events += SequenceMessage(from, to, label.first, style, head, label.second, symbol.startsWith("<<"), central, activation == '+' || centralTo, half?.second ?: false)
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
        val remaining = source.substring(start)
        val metadataLine = Regex("^acc(?:Title|Descr)\\s*:", RegexOption.IGNORE_CASE).containsMatchIn(remaining)
        val dataObject = remaining.startsWith("links ",true) || remaining.startsWith("properties ",true)
        val preserveHash = metadataLine || remaining.startsWith("box ",true) || remaining.startsWith("link ",true)
        var inMetadata=false; var quote: Char?=null; var escaped=false
        while(offset<source.length) {
            val c=source[offset]
            if(inMetadata) {
                if(escaped) escaped=false
                else if(c=='\\' && quote!=null) escaped=true
                else if(quote!=null) { if(c==quote) quote=null }
                else if(c=='\'' || c=='"') quote=c
                else if(c=='}') inMetadata=false
            } else {
                if(c in "\r\n" || !metadataLine && (c==';' || c=='#' && !preserveHash || source.startsWith("%%",offset))) break
                if(source.startsWith("@{",offset)) { offset+=2; inMetadata=true; continue }
                if(c=='{' && dataObject) { offset++; inMetadata=true; continue }
            }
            offset++
        }
        return source.substring(start, offset).trim()
    }
    private fun check(condition: Boolean, message: String) { if (!condition) fail(message) }
    private fun fail(message: String): Nothing = throw SyntaxError(message)
    private class SyntaxError(message: String) : Exception(message)
    private companion object {
        val BOX_COLOR = Regex("^(?:rgba?\\([^)]*\\)|#[0-9a-fA-F]+)")
        val METADATA = Regex("(accTitle|accDescr)\\s*:\\s*(.*)", RegexOption.IGNORE_CASE)
        val ALIAS = Regex("\\s+[aA][sS]\\s+")
        val NUMBER = Regex("(?:[0-9]+(?:\\.[0-9]{1,2})?|\\.[0-9]{1,2})")
        val WRAP = Regex("^:?(?:no)?wrap:")
        val HALF_ARROWS = mapOf(
            "-|\\" to (SequenceArrowHead.HALF_FILLED_TOP to false),
            "--|\\" to (SequenceArrowHead.HALF_FILLED_TOP to false),
            "-|/" to (SequenceArrowHead.HALF_FILLED_BOTTOM to false),
            "--|/" to (SequenceArrowHead.HALF_FILLED_BOTTOM to false),
            "-\\\\" to (SequenceArrowHead.HALF_OPEN_TOP to false),
            "--\\\\" to (SequenceArrowHead.HALF_OPEN_TOP to false),
            "-//" to (SequenceArrowHead.HALF_OPEN_BOTTOM to false),
            "--//" to (SequenceArrowHead.HALF_OPEN_BOTTOM to false),
            "/|-" to (SequenceArrowHead.HALF_FILLED_TOP to true),
            "/|--" to (SequenceArrowHead.HALF_FILLED_TOP to true),
            "\\|-" to (SequenceArrowHead.HALF_FILLED_BOTTOM to true),
            "\\|--" to (SequenceArrowHead.HALF_FILLED_BOTTOM to true),
            "//-" to (SequenceArrowHead.HALF_OPEN_TOP to true),
            "//--" to (SequenceArrowHead.HALF_OPEN_TOP to true),
            "\\\\-" to (SequenceArrowHead.HALF_OPEN_BOTTOM to true),
            "\\\\--" to (SequenceArrowHead.HALF_OPEN_BOTTOM to true)
        )
        val ARROWS = listOf("<<-->>", "<<->>", "-->>", "->>", "-->", "->", "--x", "-x", "--)", "-)") + HALF_ARROWS.keys
    }
}
