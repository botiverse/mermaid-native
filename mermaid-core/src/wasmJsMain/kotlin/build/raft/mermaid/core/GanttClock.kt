package build.raft.mermaid.core

import kotlin.js.JsFun

@JsFun("() => Date.now()")
private external fun currentTimeMillis(): Double

internal actual fun ganttCurrentEpochMillis(): Long = currentTimeMillis().toLong()
