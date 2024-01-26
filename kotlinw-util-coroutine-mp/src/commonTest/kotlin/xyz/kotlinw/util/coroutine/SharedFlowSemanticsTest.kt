package xyz.kotlinw.util.coroutine

import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlinw.util.coroutine.cancelAll
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class SharedFlowSemanticsTest {

    @Test
    fun testSlowSharedFlowCollectors() = runTest {
        withContext(Dispatchers.Default) {
            val sharedFlow = MutableSharedFlow<String>(extraBufferCapacity = 0, onBufferOverflow = SUSPEND)

            val collectors = (1..5).map { collectorId ->
                launch(start = UNDISPATCHED) {
                    sharedFlow.collect { value ->
                        println("#$collectorId start: $value")
                        delay(100.milliseconds * collectorId)
                        println("#$collectorId end: $value")
                    }
                }
            }

            repeat(3) {
                sharedFlow.emit(Char('a'.code + it).toString())
            }

            delay(350)

            collectors.cancelAll()
        }
    }

    @Test
    fun testSlowStateFlowCollectors() = runTest {
        withContext(Dispatchers.Default) {
            val stateFlow = MutableStateFlow("x")

            val collectors = (1..5).map { collectorId ->
                launch(start = UNDISPATCHED) {
                    stateFlow.collect { value ->
                        println("#$collectorId start: $value")
                        delay(100.milliseconds * collectorId)
                        println("#$collectorId end: $value")
                    }
                }
            }

            repeat(3) {
                stateFlow.emit(Char('a'.code + it).toString())
            }

            delay(350)

            collectors.cancelAll()
        }
    }

    @Test
    fun testSlowStateFlowCollectors2() = runTest {
        withContext(Dispatchers.Default) {
            val stateFlow = MutableStateFlow("x")

            launch(start = UNDISPATCHED) {
                stateFlow.collect {
                    println("start: $it")
                    delay(100.milliseconds)
                    println("end: $it")
                }
            }

            stateFlow.emit("a")
            delay(50.milliseconds)
            stateFlow.emit("b")
            delay(50.milliseconds)
            stateFlow.emit("c")
            delay(50.milliseconds)

            delay(200.milliseconds)
        }
    }

    // https://github.com/Kotlin/kotlinx.coroutines/issues/2603
    @Test
    fun testIssue2603() = runTest {
        withContext(Dispatchers.Default) {
            val numbers = MutableSharedFlow<Int>()
            val job = launch {
                numbers.collect {
                    delay(1000)
                    println("$it collected")
                }
            }

            launch {
                delay(1000)
                repeat(3) {
                    println("emit $it")
                    numbers.emit(it)
                }
                job.cancel()
            }
        }
    }

    @Test
    fun testMap() = runTest {
        val stateFlow = MutableStateFlow(1)
        stateFlow.test {
            assertEquals(1, awaitItem())
            cancel()
        }
        stateFlow.map { it * 2 }.test {
            assertEquals(2, awaitItem())
            cancel()
        }
    }
}
