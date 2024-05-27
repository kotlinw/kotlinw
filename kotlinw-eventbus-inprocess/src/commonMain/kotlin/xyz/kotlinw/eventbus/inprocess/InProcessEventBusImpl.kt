package xyz.kotlinw.eventbus.inprocess

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import xyz.kotlinw.eventbus.inprocess.EventBusBufferOverflowStrategy.DROP_LATEST
import xyz.kotlinw.eventbus.inprocess.EventBusBufferOverflowStrategy.DROP_OLDEST
import xyz.kotlinw.eventbus.inprocess.EventBusBufferOverflowStrategy.REJECT
import xyz.kotlinw.eventbus.inprocess.EventBusBufferOverflowStrategy.SUSPEND

enum class EventBusBufferOverflowStrategy {

    /**
     * Suspend on buffer overflow.
     */
    SUSPEND,

    /**
     * Drop the oldest event in the buffer on overflow, add the new event to the buffer, do not suspend.
     */
    DROP_OLDEST,

    /**
     * Drop the event that is being added to the buffer right now on buffer overflow (so that buffer contents stay the same), do not suspend.
     */
    DROP_LATEST,

    /**
     * Reject the new event, throw an [EventBusBufferOverflowException].
     */
    REJECT
}

class EventBusBufferOverflowException : RuntimeException()

/**
 * [InProcessEventBus] implementation backed by a [SharedFlow].
 *
 * @param bufferCapacity The capacity of the dispatch buffer used for buffering events. Defaults to 1000.
 * @param bufferOverflowStrategy strategy to handle fast event publishers and slow event handlers
 *
 * @throws IllegalArgumentException if [bufferCapacity] is negative.
 */
internal class InProcessEventBusImpl<E: LocalEvent>(
    bufferCapacity: Int = 1000,
    bufferOverflowStrategy: EventBusBufferOverflowStrategy = SUSPEND
) : InProcessEventBus<E> {

    init {
        require(bufferCapacity >= 0)
    }

    private val _events =
        MutableSharedFlow<E>(
            extraBufferCapacity = bufferCapacity,
            onBufferOverflow = when (bufferOverflowStrategy) {
                SUSPEND -> BufferOverflow.SUSPEND
                DROP_OLDEST -> BufferOverflow.DROP_OLDEST
                DROP_LATEST -> BufferOverflow.DROP_LATEST
                REJECT -> BufferOverflow.SUSPEND
            }
        )

    private val isRejectOnBufferOverflow = bufferOverflowStrategy == REJECT

    private val events = _events.asSharedFlow()

    override suspend fun publish(event: E) {
        if (!_events.tryEmit(event)) {
            if (isRejectOnBufferOverflow) {
                throw EventBusBufferOverflowException()
            } else {
                _events.emit(event)
            }
        }
    }

    override fun events(): Flow<E> = events
}

/**
 * Creates an [InProcessEventBus] instance.
 *
 * @param bufferCapacity The capacity of the dispatch buffer used for buffering events. Defaults to 1000.
 * @throws IllegalArgumentException if [bufferCapacity] is negative.
 */
fun <E: LocalEvent> InProcessEventBus(
    bufferCapacity: Int = 1000,
    bufferOverflowStrategy: EventBusBufferOverflowStrategy = SUSPEND
): InProcessEventBus<E> =
    InProcessEventBusImpl(bufferCapacity, bufferOverflowStrategy)
