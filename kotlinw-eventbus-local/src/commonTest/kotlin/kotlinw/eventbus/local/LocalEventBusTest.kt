package kotlinw.eventbus.local

import kotlinw.logging.platform.PlatformLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LocalEventBusTest {

    @Test
    fun testEventDispatch() =
        runTest {
            class Event1
            class Event2

            val eventBus = LocalEventBusImpl()

            var event1Count = 0
            val job1 = eventBus.on<Event1>(this) { event1Count++ }

            var event2Count = 0
            val job2 = eventBus.on<Event2>(this) { event2Count++ }

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

    @Test
    fun testEventHandlerExecutionModel() =
        runTest {
            class Event

            val eventBus = LocalEventBusImpl()

            withContext(Dispatchers.Default) {
                var eventCount = 0
                val job = eventBus.on<Event>(this) {
                    eventCount++
                    delay(10.milliseconds)
                }

                yield()
                assertEquals(0, eventCount)

                repeat(10) {
                    eventBus.dispatch(Event())
                }

                delay(25.milliseconds)
                assertEquals(2, eventCount)
                assertTrue(job.isActive)

                job.cancel()
            }
        }
}
