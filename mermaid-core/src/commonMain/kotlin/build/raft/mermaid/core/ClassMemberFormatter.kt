package build.raft.mermaid.core

/** Semantic member text and decorations shared by every Native renderer. */
public data class ClassMemberDisplay(val text: String, val italic: Boolean = false, val underline: Boolean = false)

public object ClassMemberFormatter {
    public fun format(input: String, isMethod: Boolean): ClassMemberDisplay {
        var visibility = ""
        var classifier = ""
        var id: String
        var parameters = ""
        var returnType = ""
        if (isMethod) {
            val match = Regex("([#+~-])?(.+)\\((.*)\\)([\\s$*])?(.*)([$*])?").find(input)
                ?: return ClassMemberDisplay(input)
            visibility = match.groupValues[1].trim()
            id = match.groupValues[2]
            parameters = match.groupValues[3].trim()
            classifier = match.groupValues[4].trim()
            returnType = match.groupValues[5].trim()
            if (classifier.isEmpty() && returnType.lastOrNull() in listOf('$', '*')) {
                classifier = returnType.takeLast(1); returnType = returnType.dropLast(1)
            }
        } else {
            if (input.firstOrNull() in listOf('#', '+', '~', '-')) visibility = input.take(1)
            if (input.lastOrNull() in listOf('$', '*')) classifier = input.takeLast(1)
            id = input.substring(visibility.length, input.length - classifier.length)
        }
        id = (if (id.startsWith(' ')) " " else "") + id.trim()
        val text = buildString {
            append(visibility); append(generics(id))
            if (isMethod) {
                append('('); append(generics(parameters.trim())); append(')')
                if (returnType.isNotEmpty()) { append(" : "); append(generics(returnType)) }
            }
        }.trim()
        return ClassMemberDisplay(text, italic = classifier == "*", underline = classifier == "$")
    }

    private fun generics(value: String): String {
        val parts = mutableListOf<String>()
        var start = 0
        value.forEachIndexed { index, c -> if (c == ',') { parts += value.substring(start, index); parts += ","; start = index + 1 } }
        parts += value.substring(start)
        val output = mutableListOf<String>()
        var i = 0
        while (i < parts.size) {
            var part = parts[i]
            if (part == "," && i > 0 && i + 1 < parts.size && parts[i - 1].count { it == '~' } == 1 && parts[i + 1].count { it == '~' } == 1) {
                part = parts[i - 1] + "," + parts[i + 1]; i++; output.removeAt(output.lastIndex)
            }
            output += genericSet(part); i++
        }
        return output.joinToString("")
    }

    private fun genericSet(value: String): String {
        val count = value.count { it == '~' }
        if (count <= 1) return value
        val prefix = count % 2 != 0 && value.startsWith('~')
        val chars = (if (prefix) value.drop(1) else value).toCharArray()
        var first = chars.indexOf('~'); var last = chars.lastIndexOf('~')
        while (first >= 0 && first != last) {
            chars[first] = '<'; chars[last] = '>'
            first = chars.indexOf('~'); last = chars.lastIndexOf('~')
        }
        return (if (prefix) "~" else "") + chars.concatToString()
    }
}
