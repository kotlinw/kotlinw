package kotlinw.eventbus.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @param dispatchBufferCapacity the number of buffered events; [dispatch] does not suspend while there is remaining buffer space (cannot be negative)
 */
class LocalEventBusImpl(
    dispatchBufferCapacity: Int = 1000
) : LocalEventBus {

    init {
        require(dispatchBufferCapacity >= 0)
    }

    private val _events = MutableSharedFlow<LocalEvent>(extraBufferCapacity = dispatchBufferCapacity)

    private val events = _events.asSharedFlow()

    override suspend fun dispatch(event: LocalEvent) {
        _events.emit(event)
    }

    override suspend fun on(
        eventPredicate: (LocalEvent) -> Boolean,
        handler: suspend (LocalEvent) -> Unit
    ): Nothing {
        events.collect { event ->
            if (eventPredicate(event)) {
                handler(event)
            }
        }
    }
}
