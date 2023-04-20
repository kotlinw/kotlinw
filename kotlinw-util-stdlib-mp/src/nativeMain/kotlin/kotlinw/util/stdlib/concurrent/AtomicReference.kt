package kotlinw.util.stdlib.concurrent

actual class AtomicReference<T> actual constructor(initialValue: T) {
    actual companion object {

    }

    actual fun get(): T {
        TODO("Not yet implemented")
    }

    actual fun set(value: T) {
        TODO("Not yet implemented")
    }

    actual fun getAndUpdate(updater: (T) -> T): T {
        TODO("Not yet implemented")
    }

    actual fun getAndSet(newValue: T): T {
        TODO("Not yet implemented")
    }

    actual fun updateAndGet(updater: (T) -> T): T {
        TODO("Not yet implemented")
    }

}
