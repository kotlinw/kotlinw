package xyz.kotlinw.eventbus.inprocess.example

import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import xyz.kotlinw.eventbus.inprocess.asyncOn

data class MessageEvent(val message: String)

data class SomeOtherEvent(val number: Int)

class ExampleHost {

    @Test
    fun dealingWithSlowEventHandlers() = runTest {
        withContext(Dispatchers.Default) {
            val eventBus = InProcessEventBus(5)

            val verySlowEventHandlerJob = eventBus.asyncOn<MessageEvent>(this) {
                println("> verySlowEventHandler: ${it.message}")
                delay(10)
                println("< verySlowEventHandler: ${it.message}")
            }

            val veryFastEventHandlerJob = eventBus.asyncOn<MessageEvent>(this) {
                println("> veryFastEventHandler: ${it.message}")
                delay(1)
                println("< veryFastEventHandler: ${it.message}")
            }

            (1..10).forEach {
                println("--> Publish: $it")
                eventBus.publish(MessageEvent(it.toString()))
            }

            delay(5000)
            verySlowEventHandlerJob.cancel()
            veryFastEventHandlerJob.cancel()
        }
    }
}