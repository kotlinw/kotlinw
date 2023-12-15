package xyz.kotlinw.util.coroutine

import kotlin.test.Test
import kotlinw.util.coroutine.cancelAll
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
                        delay(100)
                        println("#$collectorId end: $value")
                    }
                }
            }

            repeat(3) {
                sharedFlow.emit(Char('a'.code + it).toString())
            }

            delay(500)

            collectors.cancelAll()
        }
    }

    @Test
    fun testSlowStateFlowCollectors() = runTest {
        withContext(Dispatchers.Default) {
            val sharedFlow = MutableStateFlow("x")

            val collectors = (1..5).map { collectorId ->
                launch(start = UNDISPATCHED) {
                    sharedFlow.collect { value ->
                        println("#$collectorId start: $value")
                        delay(100)
                        println("#$collectorId end: $value")
                    }
                }
            }

            repeat(3) {
                sharedFlow.emit(Char('a'.code + it).toString())
            }

            delay(500)

            collectors.cancelAll()
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
}
