@file:JvmName("ThreadLocalJvm")

package kotlinw.util

import kotlin.jvm.JvmName

expect class ThreadLocal<T>() {
    fun get(): T?

    fun set(value: T?)

    fun remove()
}

inline var <T> ThreadLocal<T>.value: T?
    get() = get()
    set(value) {
        if (value == null) {
            remove()
        } else {
            set(value)
        }
    }

inline fun <T, R> ThreadLocal<T>.runWith(threadLocalValue: T?, block: () -> R): R {
    val previousValue = value
    return try {
        value = threadLocalValue
        block()
    } finally {
        value = previousValue
    }
}
