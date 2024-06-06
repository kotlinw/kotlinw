package xyz.kotlinw.util.coroutine

import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class FlowSemanticsTest {

    @Test
    fun testFlowCompletedWithStateIn() = runTest {
        val sourceCoroutineScope = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext.job))

        val stateFlow = flow {
            emit(1)
            delay(1.seconds)
            emit(2)
            delay(1.seconds)
            emit(3)
            delay(1.seconds)
        }
            .onStart { println(">> start: original") }
            .onCompletion { println(">> completion: original") }
            .stateIn(sourceCoroutineScope)
            .onStart { println(">> start: stateIn()") }
            .onCompletion { println(">> completion: stateIn()") }

        listOf("a", "b").forEach { executor ->
            launch {
                channelFlow {
                    sourceCoroutineScope.launch {
                        stateFlow.collect {
                            send(it)
                        }
                    }.join()
                }
                    .collect {
                        println("$executor: $it")
                    }
            }
        }

        delay(5.seconds)
        println(">> shutdown")
        sourceCoroutineScope.cancel()
    }
}
