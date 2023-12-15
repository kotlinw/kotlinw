package kotlinw.eventbus.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * [InProcessEventBus] implementation backed by a [SharedFlow].
 *
 * @param bufferCapacity The capacity of the dispatch buffer used for buffering events.
 *                               Defaults to 1000.
 *
 * @throws IllegalArgumentException if [bufferCapacity] is negative.
 */
internal class InProcessEventBusImpl(
    bufferCapacity: Int = 1000
) : InProcessEventBus {

    init {
        require(bufferCapacity >= 0)
    }

    private val _events = MutableSharedFlow<LocalEvent>(extraBufferCapacity = bufferCapacity)

    private val events = _events.asSharedFlow()

    override suspend fun publish(event: LocalEvent) {
        _events.emit(event)
    }

    override suspend fun on(
        eventPredicate: (LocalEvent) -> Boolean,
        handler: suspend (LocalEvent) -> Unit
    ): Nothing =
        events.collect {
            if (eventPredicate(it)) {
                handler(it)
            }
        }
}

/**
 * Creates an [InProcessEventBus] instance.
 *
 * @param bufferCapacity The capacity of the dispatch buffer used for buffering events. Defaults to 1000.
 * @throws IllegalArgumentException if [bufferCapacity] is negative.
 */
fun InProcessEventBus(bufferCapacity: Int = 1000): InProcessEventBus = InProcessEventBusImpl(bufferCapacity)
