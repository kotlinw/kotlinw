package kotlinw.util.stdlib

inline var <T> ThreadLocal<T>.value: T?
    get() = get()
    set(value) {
        if (value == null) {
            remove()
        } else {
            set(value)
        }
    }

inline fun <T, R> ThreadLocal<T>.withThreadLocal(threadLocalValue: T, block: () -> R): R {
    val previousValue = value
    return try {
        value = threadLocalValue
        block()
    } finally {
        value = previousValue
    }
}
