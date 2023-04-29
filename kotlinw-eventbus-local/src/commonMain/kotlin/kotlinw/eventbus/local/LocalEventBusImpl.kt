package kotlinw.eventbus.local

import kotlinw.logging.api.LoggerFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LocalEventBusImpl(
    loggerFactory: LoggerFactory,
    dispatchBufferCapacity: Int = 1000
) : LocalEventBus {

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
