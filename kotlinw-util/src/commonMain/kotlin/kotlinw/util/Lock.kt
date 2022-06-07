package kotlinw.util

expect class Lock() {
    fun <T> withLock(block: () -> T): T
}
