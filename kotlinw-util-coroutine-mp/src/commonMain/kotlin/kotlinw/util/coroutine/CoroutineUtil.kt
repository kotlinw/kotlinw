package kotlinw.util.coroutine

import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.withContext
import xyz.kotlinw.util.stdlib.runCatchingCleanup

fun CoroutineScope.createNestedScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    this.coroutineContext.createNestedScope(additionalCoroutineContext)

fun CoroutineContext.createNestedScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    CoroutineScope(
        this +
                Job(job) +
                additionalCoroutineContext
    )

fun CoroutineScope.createNestedSupervisorScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    this.coroutineContext.createNestedSupervisorScope(additionalCoroutineContext)

fun CoroutineContext.createNestedSupervisorScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    CoroutineScope(
        this +
                SupervisorJob(job) +
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
