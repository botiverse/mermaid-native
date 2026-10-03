package build.raft.mermaid.core

/** Mutable journey editor model. Read operations return detached, idempotent snapshots. */
public class JourneyDocument {
    private val sectionNames = mutableListOf<String>()
    private val entries = mutableListOf<JourneyDocumentTask>()
    private var currentSection = ""
    public var title: String = ""
    public var accessibilityTitle: String = ""
    public var accessibilityDescription: String = ""

    public fun clear() {
        sectionNames.clear(); entries.clear(); currentSection = ""
        title = ""; accessibilityTitle = ""; accessibilityDescription = ""
    }

    public fun addSection(name: String) { sectionNames += name; currentSection = name }

    /** Accept the colon-prefixed payload delivered by the Journey grammar. */
    public fun addTask(description: String, taskData: String) {
        val pieces = taskData.drop(1).split(':')
        val rawScore = pieces.first().trim()
        val score = if (rawScore.isEmpty()) 0 else rawScore.toIntOrNull()
            ?: throw IllegalArgumentException("Expected an integer journey score")
        val people = if (pieces.size == 1) emptyList() else pieces[1].split(',').map { it.trim() }
        entries += JourneyDocumentTask(description, score, people, currentSection, sectionNames.lastIndex)
    }

    public fun sections(): List<String> = sectionNames.toList()
    public fun tasks(): List<JourneyDocumentTask> = entries.map { it.copy(people = it.people.toList()) }
    public fun actors(): List<String> = entries.flatMap { it.people }.distinct().sorted()

    public fun diagram(): UserJourneyDiagram {
        val sections = buildList {
            val unsectioned = entries.filter { it.sectionIndex < 0 }
            if (unsectioned.isNotEmpty()) add(UserJourneySection("", unsectioned.map { it.task() }))
            sectionNames.forEachIndexed { index, name ->
                add(UserJourneySection(name, entries.filter { it.sectionIndex == index }.map { it.task() }))
            }
        }
        return UserJourneyDiagram(title.takeIf { it.isNotEmpty() }, sections,
            accessibilityTitle.takeIf { it.isNotEmpty() }, accessibilityDescription.takeIf { it.isNotEmpty() })
    }
}

public data class JourneyDocumentTask(
    val description: String, val score: Int, val people: List<String>, val section: String,
    internal val sectionIndex: Int,
) {
    internal fun task(): UserJourneyTask = UserJourneyTask(description, score, people.toList())
}
