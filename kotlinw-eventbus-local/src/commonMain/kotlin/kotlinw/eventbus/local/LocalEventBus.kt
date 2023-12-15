package kotlinw.eventbus.local

import kotlinw.xyz.kotlinw.stdlib.internal.ReplaceWithContextReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

typealias LocalEvent = Any

/**
 * Event bus for local (in-process) events.
 */
sealed interface LocalEventBus {

    suspend fun dispatch(event: LocalEvent)

    suspend fun <E: LocalEvent> on(
        eventPredicate: (LocalEvent) -> Boolean = { true },
        handler: suspend (E) -> Unit
    ): Nothing
}

fun LocalEventBus.launchDispatch(
    @ReplaceWithContextReceiver coroutineScope: CoroutineScope,
    event: LocalEvent
): Job =
    coroutineScope.launch(start = UNDISPATCHED) {
        dispatch(event)
    }

suspend inline fun <reified E : LocalEvent> LocalEventBus.launchEventHandler(
    handlerCoroutineScope: CoroutineScope,
    noinline eventPredicate: (LocalEvent) -> Boolean = { it is E },
    noinline handler: suspend (E) -> Unit
): Job =
    handlerCoroutineScope.launch(start = UNDISPATCHED) {
        on(eventPredicate, handler)
    }

suspend inline fun <reified E : LocalEvent, T> LocalEventBus.once(
    noinline eventPredicate: (LocalEvent) -> Boolean = { it is E},
    crossinline handler: suspend (E) -> T
): T {
    try {
        on<E>(eventPredicate) { event ->
            throw ControlledEventCollectingStop(handler(event))
        }
    } catch (e: ControlledEventCollectingStop) {
        @Suppress("UNCHECKED_CAST")
        return e.result as T
    } catch (e: Throwable) {
        throw e
    }
}

@PublishedApi
internal class ControlledEventCollectingStop(val result: Any?) : CancellationException(null as String?)

inline fun <reified E : LocalEvent, T> LocalEventBus.asyncOnce(
    @ReplaceWithContextReceiver handlerCoroutineScope: CoroutineScope,
    noinline eventPredicate: (LocalEvent) -> Boolean = { it is E },
    crossinline handler: suspend (E) -> T
): Deferred<T> =
    handlerCoroutineScope.async(start = UNDISPATCHED) {
        once(eventPredicate, handler)
    }
