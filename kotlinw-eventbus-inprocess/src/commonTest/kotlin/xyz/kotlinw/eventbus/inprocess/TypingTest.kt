package xyz.kotlinw.eventbus.inprocess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import xyz.kotlinw.eventbus.inprocess.TypingTest.CustomBaseEvent.CustomEventA
import xyz.kotlinw.eventbus.inprocess.TypingTest.CustomBaseEvent.CustomEventB
import xyz.kotlinw.eventbus.inprocess.TypingTest.CustomBaseEvent.CustomSpecialEvent
import xyz.kotlinw.eventbus.inprocess.TypingTest.CustomBaseEvent.CustomSpecialEvent.CustomEventC

class TypingTest {

    sealed interface CustomBaseEvent {

        data object CustomEventA : CustomBaseEvent

        data object CustomEventB : CustomBaseEvent

        sealed interface CustomSpecialEvent : CustomBaseEvent {

            data object CustomEventC : CustomSpecialEvent
        }
    }

    @Test
    fun testTyping() = runTest {
        var localEventCount = 0
        var customBaseEventCount = 0
        var customEventACount = 0
        var customEventBCount = 0
        var customSpecialEventCount = 0
        var customEventCCount = 0

        val eventBus = InProcessEventBus<LocalEvent>()

        val job = launch(start = UNDISPATCHED) {
            with(eventBus) {
                launch(start = UNDISPATCHED) {
                    on<LocalEvent> { localEventCount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomBaseEvent> { customBaseEventCount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomEventA> { customEventACount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomEventB> { customEventBCount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomSpecialEvent> { customSpecialEventCount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomEventC> { customEventCCount++ }
                }
            }
        }

        eventBus.publish(object : Any() {})
        eventBus.publish(CustomEventA)
        eventBus.publish(CustomEventB)
        eventBus.publish(CustomEventC)

        delay(100)
        job.cancel()

        assertEquals(4, localEventCount)
        assertEquals(3, customBaseEventCount)
        assertEquals(1, customEventACount)
        assertEquals(1, customEventBCount)
        assertEquals(1, customSpecialEventCount)
        assertEquals(1, customEventCCount)
    }

    @Test
    fun testTypingAndConstrain() = runTest {
        var customBaseEventCount = 0
        var customEventACount = 0
        var customEventBCount = 0
        var customSpecialEventCount = 0
        var customEventCCount = 0

        val globalEventBus = InProcessEventBus<LocalEvent>()
        val eventBus = globalEventBus.constrain<CustomBaseEvent, _>()

        val job = launch(start = UNDISPATCHED) {
            with(eventBus) {
                launch(start = UNDISPATCHED) {
                    on<CustomBaseEvent> { customBaseEventCount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomEventA> { customEventACount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomEventB> { customEventBCount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomSpecialEvent> { customSpecialEventCount++ }
                }
                launch(start = UNDISPATCHED) {
                    on<CustomEventC> { customEventCCount++ }
                }
            }
        }

        // There are no listeners for this event, we only check that the existing listeners do not crash
        globalEventBus.publish(object : Any() {})

        eventBus.publish(CustomEventA)
        eventBus.publish(CustomEventB)
        eventBus.publish(CustomEventC)

        delay(100)
        job.cancel()

        assertEquals(3, customBaseEventCount)
        assertEquals(1, customEventACount)
        assertEquals(1, customEventBCount)
        assertEquals(1, customSpecialEventCount)
        assertEquals(1, customEventCCount)
    }

    @Suppress("UNREACHABLE_CODE")
    fun testTypingCompilationOnly1() = runTest {
        val eventBus = InProcessEventBus<CustomBaseEvent>()
        with(eventBus) {
            // OK, it does not compile: on<LocalEvent> { }
            on<CustomBaseEvent> { }
            on<CustomEventA> { }
            on<CustomEventB> { }
            on<CustomSpecialEvent> { }
            on<CustomEventC> { }

            // OK, it does not compile: eventBus.publish(object: Any() {})
            eventBus.publish(CustomEventA)
            eventBus.publish(CustomEventB)
            eventBus.publish(CustomEventC)
        }
    }

    @Suppress("UNREACHABLE_CODE")
    fun testTypingCompilationOnly2() = runTest {
        val a: List<Int> = mutableListOf()
        val globalEventBus = InProcessEventBus<LocalEvent>()
        val eventBus = globalEventBus.constrain<CustomSpecialEvent, _>()
        with(eventBus) {
            // OK, it does not compile: on<LocalEvent> { }
            // OK, it does not compile: on<CustomBaseEvent> { }
            // OK, it does not compile: on<CustomEventA> { }
            // OK, it does not compile: on<CustomEventB> { }
            on<CustomSpecialEvent> { }
            on<CustomEventC> { }

            // OK, it does not compile: eventBus.publish(object: Any() {})
            // OK, it does not compile: eventBus.publish(CustomEventA)
            // OK, it does not compile: eventBus.publish(CustomEventB)
            eventBus.publish(CustomEventC)
        }
    }
}
