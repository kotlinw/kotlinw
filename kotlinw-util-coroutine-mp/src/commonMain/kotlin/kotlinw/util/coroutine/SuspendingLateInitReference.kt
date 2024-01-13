package kotlinw.util.coroutine

import kotlin.concurrent.Volatile
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinw.util.coroutine.SuspendingLateInitHolderReferenceImpl.Status.Initialized
import kotlinw.util.coroutine.SuspendingLateInitHolderReferenceImpl.Status.Invalidated
import kotlinw.util.coroutine.SuspendingLateInitHolderReferenceImpl.Status.Uninitalized
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SuspendingLateInitReference<T : Any> {

    suspend fun get(): T
}

interface SuspendingLateInitReferenceHolder<T : Any> : SuspendingLateInitReference<T> {

    suspend fun initialize(value: T)

    suspend fun invalidate()
}

class SuspendingLateInitHolderReferenceImpl<T : Any> : SuspendingLateInitReferenceHolder<T> {

    enum class Status {
        Uninitalized, Initialized, Invalidated
    }

    private val lock = Mutex()

    @Volatile
    private var status: Status = Uninitalized

    @Volatile
    private var value: T? = null

    private inline val nonNullValue get() = value!!

    private val coroutinesAwaitingInitialization = atomic(persistentListOf<Continuation<T>>())

    override suspend fun initialize(value: T) {
        lock.withLock {
            check(status == Uninitalized)
            this.value = value
            status = Initialized

            coroutinesAwaitingInitialization.value.forEach {
                it.resume(value)
            }

            coroutinesAwaitingInitialization.value = persistentListOf()
        }
    }

    override suspend fun invalidate() {
        lock.withLock {
            status = Invalidated
            value = null
        }
    }

    override suspend fun get(): T {
        lock.lock()
        val statusSnapshot = status
        return when (statusSnapshot) {
            Initialized -> {
                val value = nonNullValue
                lock.unlock()
                value
            }

            Invalidated -> {
                lock.unlock()
                throw IllegalStateException("Reference is invalidated.")
            }

            Uninitalized -> {
                suspendCancellableCoroutine { continuation ->
                    coroutinesAwaitingInitialization.update {
                        it.add(continuation)
                    }
                    lock.unlock()
                }
            }
        }
    }
}
