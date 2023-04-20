package kotlinw.util.stdlib

@Deprecated("Deprecated because of incorrect semantics on single-threaded platforms.")
actual class ThreadLocal<T> actual constructor() {

    actual fun get(): T? {
        TODO("Not yet implemented")
    }

    actual fun set(value: T?) {
        TODO("Not yet implemented")
    }

    actual fun remove() {
        TODO("Not yet implemented")
    }
}
