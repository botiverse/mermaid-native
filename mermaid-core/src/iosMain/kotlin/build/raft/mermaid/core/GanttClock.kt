package build.raft.mermaid.core

import platform.Foundation.NSDate

internal actual fun ganttCurrentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
