package build.raft.mermaid.core

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun ganttCurrentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
