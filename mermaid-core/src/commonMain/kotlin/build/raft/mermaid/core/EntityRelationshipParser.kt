package build.raft.mermaid.core

/** Token cursor for ER's whitespace-delimited grammar (including inline attribute blocks). */
internal class EntityRelationshipParser(private val source: String) {
    private var offset = 0
    private val entities = linkedMapOf<String, EntityDefinition>()
    private val relationships = mutableListOf<EntityRelationship>()

    fun parse(): MermaidParseResult = try {
        whitespace()
        requireSyntax(take("erDiagram"), "Expected erDiagram")
        while (true) {
            whitespace()
            if (offset == source.length) break
            val from = entityName()
            ensure(from)
            whitespace()
            when {
                take("[") -> {
                    val alias = entityName()
                    whitespace()
                    requireSyntax(take("]"), "Expected ] after entity alias")
                    if (entities.getValue(from).alias == null) {
                        entities[from] = entities.getValue(from).copy(alias = alias)
                    }
                    whitespace()
                    if (take("{")) attributes(from)
                }
                take("{") -> attributes(from)
                else -> {
                    val left = relationshipCardinality()
                    if (left != null) {
                        whitespace()
                        val identifying = when {
                            take("--") || takeWord("to") -> true
                            take("..") || take(".-") || take("-.") || takeWord("optionally to") -> false
                            else -> fail("Expected identifying or non-identifying relationship")
                        }
                        whitespace()
                        val right = cardinality() ?: fail("Expected relationship cardinality")
                        val to = entityName()
                        whitespace()
                        requireSyntax(take(":"), "Expected : and relationship label")
                        whitespace()
                        val label = if (peek() == '"') quoted('"', entity = false) else entityName()
                        ensure(to)
                        relationships += EntityRelationship(from, to, left, right, label, identifying)
                    }
                }
            }
        }
        MermaidParseResult.Success(EntityRelationshipDiagram(entities.values.toList(), relationships))
    } catch (error: SyntaxError) {
        val prefix = source.take(error.offset)
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(
            MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
            error.message ?: "Unsupported entity relationship syntax",
            SourceLocation(prefix.count { it == '\n' } + 1, error.offset - prefix.lastIndexOf('\n')),
        )))
    }

    private fun ensure(id: String) {
        if (id !in entities) entities[id] = EntityDefinition(id)
    }

    private fun attributes(id: String) {
        val attributes = mutableListOf<EntityAttribute>()
        while (true) {
            whitespace()
            if (take("}")) break
            requireSyntax(offset < source.length, "Unclosed entity declaration: $id")
            val type = attributeWord() + if (take("?")) "?" else ""
            val name = attributeWord()
            whitespace()
            val keys = mutableListOf<EntityKey>()
            fun key(): EntityKey? = EntityKey.entries.firstOrNull { it != EntityKey.NONE && takeWord(it.name) }
            key()?.let { first ->
                keys += first
                whitespace()
                while (take(",")) {
                    whitespace()
                    keys += key() ?: fail("Expected PK, FK, or UK after comma")
                    whitespace()
                }
            }
            val comment = if (peek() == '"') quoted('"', entity = false) else null
            attributes += EntityAttribute(type, name, keys.firstOrNull() ?: EntityKey.NONE, comment, keys.drop(1))
        }
        val entity = entities.getValue(id)
        entities[id] = entity.copy(attributes = entity.attributes + attributes)
    }

    private fun attributeWord(): String {
        whitespace()
        if (peek() == '`') return quoted('`', entity = false).also {
            requireSyntax(it.isNotEmpty(), "Empty attribute name or type")
        }
        val start = offset
        val first = peek()
        requireSyntax(first != null && (first in 'A'..'Z' || first in 'a'..'z' || first == '_' || first == '*' || first.code >= 0xC0), "Expected attribute type or name")
        while (peek()?.let { it.isLetterOrDigit() || it.code >= 0xC0 || it in "-_[].(),*<>~" } == true) offset++
        val value = source.substring(start, offset)
        requireSyntax(value.uppercase() !in setOf("PK", "FK", "UK"), "Attribute key requires a type and name")
        return value
    }

    private fun entityName(): String {
        whitespace()
        if (peek() == '"') return quoted('"', entity = true)
        val start = offset
        while (peek()?.let { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it.code > 127 || it in "_-*." } == true) {
            // Relationship punctuation belongs to the next token, even without spaces.
            if (source.startsWith("--", offset) || source.startsWith("..", offset) || source.startsWith(".-", offset) || source.startsWith("-.", offset)) break
            offset++
        }
        requireSyntax(offset > start, "Expected entity name")
        val name = source.substring(start, offset)
        requireSyntax(name.lowercase() !in setOf("direction", "style", "classdef", "class", "subgraph", "end", "acctitle", "accdescr", "one", "many", "to"), "Unsupported ER directive or reserved entity name: $name")
        return name
    }

    private fun relationshipCardinality(): EntityCardinality? {
        val start = offset
        val card = cardinality()
        whitespace()
        val afterCard = offset
        val hasOperator = take("--") || take("..") || take(".-") || take("-.") || takeWord("to") || takeWord("optionally to")
        offset = if (card != null && hasOperator) afterCard else start
        return card.takeIf { hasOperator }
    }

    private fun cardinality(): EntityCardinality? {
        whitespace()
        return CARDINALITIES.firstOrNull { (token, _) ->
            if (token[0].isLetterOrDigit()) takeWord(token) else take(token)
        }?.second
    }

    private fun quoted(delimiter: Char, entity: Boolean): String {
        requireSyntax(peek() == delimiter, "Expected quoted text")
        offset++
        val start = offset
        while (peek() != delimiter) {
            val char = peek() ?: fail("Unclosed quoted text")
            requireSyntax(!entity || char !in "%\r\n\u000b\b\\", "Invalid character in quoted entity name")
            offset++
        }
        val value = source.substring(start, offset++)
        requireSyntax(!entity || value.isNotEmpty(), "Empty entity name")
        return value
    }

    private fun whitespace() {
        while (offset < source.length) {
            if (source[offset].isWhitespace()) offset++
            else if (source.startsWith("%%", offset)) {
                while (offset < source.length && source[offset] != '\n') offset++
            } else break
        }
    }

    private fun peek(): Char? = source.getOrNull(offset)
    private fun take(token: String): Boolean {
        if (!source.regionMatches(offset, token, 0, token.length, ignoreCase = true)) return false
        offset += token.length
        return true
    }
    private fun takeWord(token: String): Boolean {
        val after = source.getOrNull(offset + token.length)
        if (after != null && (after.isLetterOrDigit() || after == '_')) return false
        return take(token)
    }
    private fun requireSyntax(condition: Boolean, message: String) { if (!condition) fail(message) }
    private fun fail(message: String): Nothing = throw SyntaxError(message, offset)
    private class SyntaxError(message: String, val offset: Int) : Exception(message)

    private companion object {
        val CARDINALITIES = listOf(
            "zero or one" to EntityCardinality.ZERO_OR_ONE,
            "one or zero" to EntityCardinality.ZERO_OR_ONE,
            "zero or more" to EntityCardinality.ZERO_OR_MORE,
            "zero or many" to EntityCardinality.ZERO_OR_MORE,
            "one or more" to EntityCardinality.ONE_OR_MORE,
            "one or many" to EntityCardinality.ONE_OR_MORE,
            "only one" to EntityCardinality.ONLY_ONE,
            "many(0)" to EntityCardinality.ZERO_OR_MORE,
            "many(1)" to EntityCardinality.ONE_OR_MORE,
            "many" to EntityCardinality.ZERO_OR_MORE,
            "one" to EntityCardinality.ONLY_ONE,
            "0+" to EntityCardinality.ZERO_OR_MORE,
            "1+" to EntityCardinality.ONE_OR_MORE,
            "1" to EntityCardinality.ONLY_ONE,
            "||" to EntityCardinality.ONLY_ONE,
            "o|" to EntityCardinality.ZERO_OR_ONE,
            "|o" to EntityCardinality.ZERO_OR_ONE,
            "|{" to EntityCardinality.ONE_OR_MORE,
            "}|" to EntityCardinality.ONE_OR_MORE,
            "o{" to EntityCardinality.ZERO_OR_MORE,
            "}o" to EntityCardinality.ZERO_OR_MORE,
        )
    }
}
