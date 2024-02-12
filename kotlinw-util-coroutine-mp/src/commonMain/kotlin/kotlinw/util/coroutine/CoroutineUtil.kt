package kotlinw.util.coroutine

import arrow.core.NonFatal
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import xyz.kotlinw.util.stdlib.runCatchingCleanup

fun CoroutineScope.createNestedScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    CoroutineScope(
        coroutineContext +
                Job(coroutineContext.job) +
                additionalCoroutineContext
    )

fun CoroutineScope.createNestedSupervisorScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    CoroutineScope(
        coroutineContext +
                SupervisorJob(coroutineContext.job) +
                additionalCoroutineContext
    )

suspend inline fun runCatchingNonCancellableCleanup(crossinline block: suspend () -> Unit) {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }

    withContext(NonCancellable) {
        runCatchingCleanup {
            block()
        }
    }
}
