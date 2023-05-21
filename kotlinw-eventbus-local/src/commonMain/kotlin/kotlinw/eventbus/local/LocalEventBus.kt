package kotlinw.eventbus.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

typealias LocalEvent = Any

/**
 * Event bus for local (in-process) events.
 */
sealed interface LocalEventBus {

    suspend fun dispatch(event: LocalEvent)

    suspend fun on(
        eventPredicate: (LocalEvent) -> Boolean,
        handler: suspend (LocalEvent) -> Unit
    ): Nothing
}

suspend inline fun <reified E : LocalEvent> LocalEventBus.on(
    noinline handler: suspend (E) -> Unit
): Nothing =
    on({ it is E }) { handler(it as E) }

suspend fun LocalEventBus.once(
    eventPredicate: (LocalEvent) -> Boolean,
    handler: suspend (LocalEvent) -> Unit
) {
    on(eventPredicate) { event ->
        handler(event)
        currentCoroutineContext().cancel()
    }
}

suspend inline fun <reified E : LocalEvent> LocalEventBus.once(
    noinline handler: suspend (E) -> Unit
) =
    once({ it is E }) { handler(it as E) }

fun LocalEventBus.dispatch(coroutineScope: CoroutineScope, event: LocalEvent): Job =
    coroutineScope.launch(start = UNDISPATCHED) {
        dispatch(event)
    }

fun LocalEventBus.on(
    handlerCoroutineScope: CoroutineScope,
    eventPredicate: (LocalEvent) -> Boolean,
    handler: suspend (LocalEvent) -> Unit
) =
    handlerCoroutineScope.launch(start = UNDISPATCHED) {
        on(eventPredicate, handler)
    }

suspend inline fun <reified E : LocalEvent> LocalEventBus.on(
    handlerCoroutineScope: CoroutineScope,
    noinline handler: suspend (E) -> Unit
): Job =
    handlerCoroutineScope.launch(start = UNDISPATCHED) {
        on(handler)
    }

fun LocalEventBus.once(
    handlerCoroutineScope: CoroutineScope,
    eventPredicate: (LocalEvent) -> Boolean,
    handler: suspend (LocalEvent) -> Unit
) =
    handlerCoroutineScope.launch(start = UNDISPATCHED) {
        once(eventPredicate, handler)
    }

suspend inline fun <reified E : LocalEvent> LocalEventBus.once(
    handlerCoroutineScope: CoroutineScope,
    noinline handler: suspend (E) -> Unit
): Job =
    handlerCoroutineScope.launch(start = UNDISPATCHED) {
        once(handler)
    }
