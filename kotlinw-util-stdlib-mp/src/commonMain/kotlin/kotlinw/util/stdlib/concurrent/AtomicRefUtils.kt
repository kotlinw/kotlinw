package kotlinw.util.stdlib.concurrent

import arrow.core.continuations.AtomicRef

inline var <T> AtomicRef<T>.value: T
    get() = get()
    set(value) {
        set(value)
    }
