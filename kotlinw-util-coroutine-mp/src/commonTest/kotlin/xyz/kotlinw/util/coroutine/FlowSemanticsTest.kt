package xyz.kotlinw.util.coroutine

import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class FlowSemanticsTest {

    @Test
    fun testFlowCompletedWithStateIn() = runTest {
        val testScope = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext.job))

        val stateFlow = flow {
            emit(1)
            delay(1.seconds)
            emit(2)
            delay(1.seconds)
            emit(3)
            delay(1.seconds)
        }
            .stateIn(testScope)

        testScope.launch {
            stateFlow.collect { println("a: $it") }
        }
        testScope.launch {
            stateFlow.collect { println("b: $it") }
        }

        delay(5.seconds)
        testScope.cancel()
    }
}
