package kotlinw.util.concurrent

import kotlin.reflect.KProperty

expect class AtomicReference<T>(initialValue: T) {
    companion object

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

operator fun <T> AtomicReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

operator fun <T> AtomicReference<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}
