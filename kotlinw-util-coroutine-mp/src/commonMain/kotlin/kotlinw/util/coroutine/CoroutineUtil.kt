package kotlinw.util.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.createNestedScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    CoroutineScope(
        Job(coroutineContext.job) +
                additionalCoroutineContext +
                (coroutineContext[CoroutineExceptionHandler] ?: EmptyCoroutineContext)
    )

fun CoroutineScope.createNestedSupervisorScope(additionalCoroutineContext: CoroutineContext = EmptyCoroutineContext): CoroutineScope =
    CoroutineScope(
        SupervisorJob(coroutineContext.job) +
                additionalCoroutineContext +
                (coroutineContext[CoroutineExceptionHandler] ?: EmptyCoroutineContext)
    )
