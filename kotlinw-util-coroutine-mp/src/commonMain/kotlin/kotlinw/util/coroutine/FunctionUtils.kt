package kotlinw.util.coroutine

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.Continuation
import kotlin.time.Duration

@Suppress("UNCHECKED_CAST")
fun <T> invokeSuspendFunction(continuation: Continuation<*>, block: suspend () -> T): T =
    (block as (Continuation<*>) -> T)(continuation)

suspend inline fun <V, E> retryUntilSuccessful(
    delayAfterFailure: Duration = Duration.ZERO,
    noinline onFailure: (E) -> Unit = {},
    block: () -> Result<V, E>
): V {
    while (true) {
        block()
            .onSuccess { return it }
            .onFailure(onFailure)

        currentCoroutineContext().ensureActive()
        delay(delayAfterFailure)
    }
}

suspend inline fun <V> retryUntilNoExceptionThrown(
    delayAfterException: Duration = Duration.ZERO,
    noinline onException: (Throwable) -> Unit = {},
    block: () -> V
): V =
    retryUntilSuccessful(delayAfterException, onException) {
        runCatching { block() }
    }
