package kotlinw.util.stdlib.concurrent

expect class AtomicBoolean constructor(initialValue: Boolean) {
    fun get(): Boolean

    fun set(value: Boolean)

    fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean

    fun getAndSet(newValue: Boolean): Boolean
}

inline var AtomicBoolean.value
    get() = get()
    set(value) = set(value)
