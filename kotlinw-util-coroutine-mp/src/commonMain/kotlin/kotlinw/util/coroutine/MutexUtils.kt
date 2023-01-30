package kotlinw.util.coroutine

import kotlinw.util.stdlib.debugName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

// Based on: https://elizarov.medium.com/phantom-of-the-coroutine-afc63b03a131
suspend fun <T> Mutex.withReentrantLock(block: suspend () -> T): T {
    val key = ReentrantMutexContextKey(this)

    val existingReentrantMutexContextElement = currentCoroutineContext()[key]
    return if (existingReentrantMutexContextElement != null) {
        check(existingReentrantMutexContextElement.key.mutex === this) { "Currently only one reentrant ${Mutex::class.debugName} is supported." }
        block()
    } else {
        withContext(ReentrantMutexContextElement(key)) {
            withLock {
                block()
            }
        }
    }
}

private class ReentrantMutexContextElement(
    override val key: ReentrantMutexContextKey
) : CoroutineContext.Element

private data class ReentrantMutexContextKey(
    val mutex: Mutex
) : CoroutineContext.Key<ReentrantMutexContextElement>
