@file:JvmName("LockJvm")
package kotlinw.util.stdlib

import kotlin.jvm.JvmName

expect class Lock() {
    fun lock()

    fun unlock()
}

fun <T> Lock.withLock(block: () -> T): T =
    try {
        lock()
        block()
    } finally {
        unlock()
    }
