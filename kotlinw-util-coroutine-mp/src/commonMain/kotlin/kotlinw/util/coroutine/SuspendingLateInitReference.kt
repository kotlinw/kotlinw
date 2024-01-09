package kotlinw.util.coroutine

import kotlin.concurrent.Volatile
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SuspendingLateInitReference<T : Any> {

    suspend fun get(): T
}

interface SuspendingLateInitReferenceHolder<T : Any> : SuspendingLateInitReference<T> {

    suspend fun intialize(value: T)
}

class SuspendingLateInitHolderReferenceImpl<T : Any> : SuspendingLateInitReferenceHolder<T> {

    private val lock = Mutex()

    @Volatile
    private var value: T? = null

    private inline val isInitialized get() = value != null

    private inline val nonNullValue get() = value!!

    private val coroutinesAwaitingInitialization = atomic(persistentListOf<Continuation<T>>())

    override suspend fun intialize(value: T) {
        lock.withLock {
            check(!isInitialized)
            this.value = value

            coroutinesAwaitingInitialization.value.forEach {
                it.resume(value)
            }

            coroutinesAwaitingInitialization.value = persistentListOf()
        }
    }

    override suspend fun get(): T =
        if (isInitialized) {
            nonNullValue
        } else {
            lock.lock()
            if (isInitialized) {
                lock.unlock()
                nonNullValue
            } else {
                suspendCoroutine { continuation ->
                    coroutinesAwaitingInitialization.update {
                        it.add(continuation)
                    }
                    lock.unlock()
                }
            }
        }
}
