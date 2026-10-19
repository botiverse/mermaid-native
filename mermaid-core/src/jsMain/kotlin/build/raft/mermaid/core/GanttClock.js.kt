package build.raft.mermaid.core

import kotlin.js.Date

internal actual fun ganttCurrentEpochMillis(): Long = Date.now().toLong()
