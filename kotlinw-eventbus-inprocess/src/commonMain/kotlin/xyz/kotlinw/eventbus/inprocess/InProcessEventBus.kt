package xyz.kotlinw.eventbus.inprocess

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import xyz.kotlinw.stdlib.internal.ReplaceWithContextReceiver

typealias LocalEvent = Any

/**
 * `InProcessEventBus` represents an in-process event bus.
 *
 * It allows registering event handlers and publishing events to all registered handlers.
 */
sealed interface InProcessEventBus {
    // FIXME make generic E: LocalEvent

    /**
     * Publishes a [LocalEvent] to the event bus, suspending if the event handlers are too slow and are still handling previously published events.
     *
     * @param event The event to be published.
     */
    suspend fun publish(event: LocalEvent)

    suspend fun events(): Flow<LocalEvent>
}

context(CoroutineScope)
fun InProcessEventBus.launchPublish(event: LocalEvent) =
    launch(start = UNDISPATCHED) {
        publish(event)
    }

suspend inline fun <reified E : LocalEvent> InProcessEventBus.events(
    noinline filter: (E) -> Boolean = { true }
): Flow<E> =
    events().filterIsInstance<E>().filter(filter)

/**
 * Registers an event handler for events of type [E] with the given predicate on the event bus.
 *
 * This function never completes normally, see [SharedFlow.collect] for more details.
 *
 * @param E The type of the event to handle.
 * @param filter The predicate to filter events. By default, it accepts all events of type [E].
 * @param handler The event handler function that will be executed when an event is matched by the predicate.
 */
suspend inline fun <reified E : LocalEvent> InProcessEventBus.on(
    noinline filter: (E) -> Boolean = { true },
    noinline handler: suspend (E) -> Unit
): Nothing {
    events<E>(filter).collect {
        handler(it)
    }

    // The implementation is based on SharedFlow, and SharedFlow.collect() never completes normally
    throw IllegalStateException()
}

/**
 * Registers an asynchronously running event handler on the event bus for handling events of type [E] matching the given predicate.
 *
 * This method launches a coroutine and returns a [Job] that can be used to control and monitor the execution of the event handler.
 *
 * @param E The type of the event to handle.
 * @param handlerCoroutineScope The [CoroutineScope] in which the handler will be executed.
 * @param filter The predicate to filter events. By default, it accepts all events of type [E].
 * @param handler The event handler function that will be executed when an event is matched by the predicate.
 * @return The [Job] representing the execution of the event handler.
 */
suspend inline fun <reified E : LocalEvent> InProcessEventBus.asyncOn(
    @ReplaceWithContextReceiver handlerCoroutineScope: CoroutineScope,
    noinline filter: (E) -> Boolean = { true },
    noinline handler: suspend (E) -> Unit
): Job = handlerCoroutineScope.launch(start = UNDISPATCHED) {
    @Suppress("UNCHECKED_CAST")
    on(filter, handler as suspend (LocalEvent) -> Unit)
}

/**
 * Executes the provided event handler function _at most once_ for a specific event of type [E].
 *
 * Execution is suspended until the first matching event is published to the event bus, then [handler] is executed and execution resumes with the result.
 *
 * @param E The type of the event to handle.
 * @param T The return type of the event handler function.
 * @param filter The predicate to filter events. By default, it accepts all events of type [E].
 * @param handler The event handler function that will be executed when an event is matched by the predicate.
 * @return The result of the event handler function execution.
 */
suspend inline fun <reified E : LocalEvent, T> InProcessEventBus.once(
    noinline filter: (LocalEvent) -> Boolean = { true },
    noinline handler: suspend (E) -> T
): T {
    try {
        on<E>(filter) { event ->
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

/**
 * Asynchronously executes the provided event handler function _at most once_ for a specific event of type [E].
 *
 * This method launches a coroutine and returns a [Deferred] representing the execution of the event handler.
 * The launched coroutine is suspended until the first matching event is published to the event bus, then [handler] is executed and the coroutine completes with the result.
 *
 * @param E The type of the event to handle.
 * @param T The return type of the event handler function.
 * @param handlerCoroutineScope The [CoroutineScope] in which the handler will be executed.
 * @param filter The predicate to filter events. By default, it accepts all events of type [E].
 * @param handler The event handler function that will be executed when an event is matched by the predicate.
 * @return The [Deferred] representing the execution of the event handler.
 */
inline fun <reified E : LocalEvent, T> InProcessEventBus.asyncOnce(
    @ReplaceWithContextReceiver handlerCoroutineScope: CoroutineScope,
    noinline filter: (E) -> Boolean = { true },
    noinline handler: suspend (E) -> T
): Deferred<T> =
    handlerCoroutineScope.async(start = UNDISPATCHED) {
        @Suppress("UNCHECKED_CAST")
        once(filter as (LocalEvent) -> Boolean, handler)
    }
