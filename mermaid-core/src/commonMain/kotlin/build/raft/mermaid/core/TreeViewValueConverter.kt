package build.raft.mermaid.core

/** Conversion of already validated TreeView tokens, shared with the production parser. */
public object TreeViewValueConverter {
    public fun indentation(input: String): Int = input.length
    public fun quotedName(input: String): String = input.substring(1, (input.length - 1).coerceAtLeast(1))
    public fun bareName(input: String): String = input.trimEnd(' ', '\t')
    public fun classAnnotation(input: String): String = input.trim().drop(3).trim()
    public fun iconAnnotation(input: String): String = input.trim().let { it.drop(5).dropLast(1) }
    public fun descriptionAnnotation(input: String): String = input.trim().drop(2).trim()

    /** Unknown grammar rules are intentionally unhandled, as in the upstream converter. */
    public fun convert(rule: String, input: String): Any? = when (rule) {
        "INDENTATION" -> indentation(input)
        "QUOTED_NAME" -> quotedName(input)
        "BARE_NAME" -> bareName(input)
        "CLASS_ANNOTATION" -> classAnnotation(input)
        "ICON_ANNOTATION" -> iconAnnotation(input)
        "DESC_ANNOTATION" -> descriptionAnnotation(input)
        else -> null
    }
}
