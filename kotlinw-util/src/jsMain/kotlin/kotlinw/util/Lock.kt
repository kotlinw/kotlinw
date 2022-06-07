package kotlinw.util

actual class Lock {
    actual fun <T> withLock(block: () -> T): T = block()
}
