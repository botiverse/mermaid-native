package build.raft.mermaid.core

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal actual fun ganttCurrentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
