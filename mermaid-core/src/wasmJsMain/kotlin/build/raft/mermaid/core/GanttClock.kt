package build.raft.mermaid.core

@JsFun("() => Date.now()")
private external fun currentTimeMillis(): Double

internal actual fun ganttCurrentEpochMillis(): Long = currentTimeMillis().toLong()
