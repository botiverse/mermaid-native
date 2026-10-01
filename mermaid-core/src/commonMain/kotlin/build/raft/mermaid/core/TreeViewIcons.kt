package build.raft.mermaid.core

/** Programmatic icon selection options; default trees retain their connector dots. */
public data class TreeViewIconConfig(
    val showIcons: Boolean = false,
    val defaultIconPack: String = "",
    val filenameIcons: Map<String, String> = emptyMap(),
    val extensionIcons: Map<String, String> = emptyMap(),
)

/** Resolves icon references consumed by the TreeView renderer; it does not fetch icon packs. */
public object TreeViewIcons {
    public const val BUILTIN_PREFIX: String = "mermaid-treeview"

    /** Exact filename overrides win; extension keys are lowercase, optionally prefixed by a dot. */
    public fun detectIcon(name: String, config: TreeViewIconConfig = TreeViewIconConfig()): String? {
        config.filenameIcons[name]?.takeIf { it.isNotEmpty() }?.let { return it }
        val dot = name.lastIndexOf('.')
        if (dot <= 0) return null
        val extension = name.substring(dot).lowercase()
        return config.extensionIcons[extension] ?: config.extensionIcons[extension.substring(1)]
    }

    /** Explicit annotations win, including `none`; automatic file detection requires showIcons. */
    public fun getNodeIcon(node: TreeViewNode, config: TreeViewIconConfig = TreeViewIconConfig()): String? {
        val explicit = node.iconAnnotation
        if (explicit == "none") return null
        if (!explicit.isNullOrEmpty()) return qualify(explicit, config.defaultIconPack)
        if (!config.showIcons) return null
        if (!node.directory) {
            val detected = detectIcon(node.label, config)
            if (detected == "none") return null
            if (!detected.isNullOrEmpty()) return qualify(detected, config.defaultIconPack)
        }
        return "$BUILTIN_PREFIX:${if (node.directory) "folder" else "file"}"
    }

    private fun qualify(icon: String, defaultPack: String): String = when {
        ':' in icon -> icon
        icon == "file" || icon == "folder" || defaultPack.isEmpty() -> "$BUILTIN_PREFIX:$icon"
        else -> "$defaultPack:$icon"
    }
}
