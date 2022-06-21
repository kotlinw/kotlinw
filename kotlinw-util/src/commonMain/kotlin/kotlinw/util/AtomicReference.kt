package kotlinw.util

expect class AtomicReference<T>(initialValue: T) {
    fun get(): T

    fun set(value: T)

    fun getAndUpdate(updater: (T) -> T): T

    fun getAndSet(newValue: T): T

    fun updateAndGet(updater: (T) -> T): T
}

inline var <T> AtomicReference<T>.value: T
    get() = get()
    set(value) {
        set(value)
    }

fun <T> AtomicReference<T>.update(updater: (T) -> T) = updateAndGet(updater)
