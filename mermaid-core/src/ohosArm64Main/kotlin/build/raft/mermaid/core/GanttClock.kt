package build.raft.mermaid.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
internal actual fun ganttCurrentEpochMillis(): Long = memScoped {
    val now = alloc<timeval>()
    check(gettimeofday(now.ptr, null) == 0) { "Cannot read the OHOS wall clock" }
    now.tv_sec * 1000L + now.tv_usec / 1000L
}
