package kotlinw.util

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

actual class Lock {
    private val jvmLock = ReentrantLock()

    actual fun <T> withLock(block: () -> T): T = jvmLock.withLock { block() }
}
