package kotlinw.eventbus.local

import arrow.atomic.AtomicInt
import arrow.atomic.value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@ExperimentalCoroutinesApi
class LocalEventBusTest {

    @Test
    fun testBasics() = runTest {
        class Event1
        class Event2

        val eventBus = LocalEventBusImpl()

        var event1Count = 0
        val job1 = eventBus.launchEventHandler<Event1>(this) { event1Count++ }

        var event2Count = 0
        val job2 = eventBus.launchEventHandler<Event2>(this) { event2Count++ }

        yield()

        assertEquals(0, event1Count)
        assertEquals(0, event2Count)

        assertTrue(job1.isActive)
        assertTrue(job2.isActive)

        eventBus.dispatch(Event1())
        eventBus.dispatch(Event1())
        yield()

        assertEquals(2, event1Count)
        assertEquals(0, event2Count)

        eventBus.dispatch(Event1())
        eventBus.dispatch(Event2())
        yield()

        assertEquals(3, event1Count)
        assertEquals(1, event2Count)

        job1.cancel()

        eventBus.dispatch(Event1())
        eventBus.dispatch(Event2())
        yield()

        assertEquals(3, event1Count)
        assertEquals(2, event2Count)

        job2.cancel()
    }

    private data class OneTimeEvent(val id: Int)

    @Test
    fun testOnce() = runTest {
        val eventBus: LocalEventBus = LocalEventBusImpl()

        val deferredResult = async {
            eventBus.once<OneTimeEvent, _> { it.id }
        }

        executeTestOnce(deferredResult, eventBus)
    }

    @Test
    fun testAsyncOnce() = runTest {
        val eventBus: LocalEventBus = LocalEventBusImpl()

        val deferredResult = eventBus.asyncOnce<OneTimeEvent, _>(this) {
            it.id
        }

        executeTestOnce(deferredResult, eventBus)
    }

    private suspend fun executeTestOnce(
        deferredResult: Deferred<Int>,
        eventBus: LocalEventBus
    ) {
        yield()

        assertTrue(deferredResult.isActive)
        assertFalse(deferredResult.isCompleted)
        assertFalse(deferredResult.isCancelled)

        eventBus.dispatch(OneTimeEvent(123))

        yield()

        assertFalse(deferredResult.isActive)
        assertTrue(deferredResult.isCompleted)
        assertFalse(deferredResult.isCancelled)

        assertEquals(123, deferredResult.getCompleted())
    }

    @Test
    fun testEventHandlerExecutionModel() = runTest {
        class Event

        val eventBus = LocalEventBusImpl()

        withContext(Dispatchers.Default) {
            val eventCount = AtomicInt(0)
            val job = eventBus.launchEventHandler<Event>(this) {
                eventCount.incrementAndGet()
                delay(10.milliseconds)
            }

            yield()
            assertEquals(0, eventCount.value)

            repeat(10) {
                eventBus.dispatch(Event())
            }

            delay(25.milliseconds)
            assertEquals(3, eventCount.value)
            assertTrue(job.isActive)

            job.cancel()
        }
    }
}
