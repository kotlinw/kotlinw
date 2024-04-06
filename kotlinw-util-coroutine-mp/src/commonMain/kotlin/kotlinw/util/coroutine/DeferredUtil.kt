package kotlinw.util.coroutine

import arrow.core.raise.Raise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

data class AwaitedCoroutineCancelled(val exception: CancellationException)

/**
 * Awaits for completion of this `Deferred` without blocking the thread.
 * Behaviour details:
 * - if the caller coroutine is cancelled then normal cancellation happens (no matter the awaited `Deferred`'s state)
 * - if the `Deferred` is cancelled then `AwaitedCoroutineCancelled` is raised
 * - if the `Deferred` completed successfully then its returned value is returned on every call to `awaitSafely()`
 * - if the `Deferred` completed exceptionally, the given exception is thrown.
 *
 * There is a special case when both this coroutine and the coroutine this coroutine was waiting for were cancelled.
 * In this case the current coroutine's cancellation is reported to the caller (and the `Deferred`'s cancellation is "shadowed").
 *
 * @see [Deferred.await]
 * @see [https://github.com/Kotlin/kotlinx.coroutines/issues/3658]
 * @see [https://betterprogramming.pub/the-silent-killer-thats-crashing-your-coroutines-9171d1e8f79b]
 * @see [https://stackoverflow.com/a/76261124/306047]
 */
context(Raise<AwaitedCoroutineCancelled>)
suspend fun <T> Deferred<T>.awaitSafely(): T =
    try {
        await()
    } catch (e: CancellationException) {
        if (currentCoroutineContext().isActive) {
            raise(AwaitedCoroutineCancelled(e))
        } else {
            throw e
        }
    }
