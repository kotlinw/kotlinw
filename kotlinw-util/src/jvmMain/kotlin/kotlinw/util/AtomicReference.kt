package kotlinw.util

actual class AtomicReference<T> actual constructor(initialValue: T) {
    private val wrapped = java.util.concurrent.atomic.AtomicReference(initialValue)

    actual fun get(): T = wrapped.get()

    actual fun set(value: T) = wrapped.set(value)

    actual fun getAndUpdate(updater: (T) -> T): T = wrapped.getAndUpdate(updater)

    actual fun updateAndGet(updater: (T) -> T): T = wrapped.updateAndGet(updater)
}
