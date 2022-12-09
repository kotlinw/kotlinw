package kotlinw.util.concurrent

actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    actual fun get(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun set(value: Boolean) {
    }

    actual fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    actual fun getAndSet(newValue: Boolean): Boolean {
        TODO("Not yet implemented")
    }

}
