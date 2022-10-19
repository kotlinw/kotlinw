package kotlinw.util.concurrent

actual class AtomicReference<T> actual constructor(initialValue: T) {
    actual companion object;

    private var currentValue: T = initialValue

    actual fun get(): T = currentValue

    actual fun set(value: T) {
        this.currentValue = value
    }

    actual fun getAndUpdate(updater: (T) -> T): T {
        val returnValue = currentValue
        currentValue = updater(currentValue)
        return returnValue
    }

    actual fun updateAndGet(updater: (T) -> T): T {
        currentValue = updater(currentValue)
        return currentValue
    }

    actual fun getAndSet(newValue: T): T {
        val previousValue = currentValue
        currentValue = newValue
        return previousValue
    }
}
