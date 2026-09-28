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
            append(visibility); append(MermaidText.parseGenericTypes(id))
            if (isMethod) {
                append('('); append(MermaidText.parseGenericTypes(parameters.trim())); append(')')
                if (returnType.isNotEmpty()) { append(" : "); append(MermaidText.parseGenericTypes(returnType)) }
            }
        }.trim()
        return ClassMemberDisplay(text, italic = classifier == "*", underline = classifier == "$")
    }

}
