package xyz.kotlinw.util.coroutine

import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class CoroutineSemanticsTest {

    @Test
    fun test() = runTest {

        suspend fun printLoop(id: String) {
            (0..5).forEach {
                println("$id: $it")
                delay(1.seconds)
            }
        }

        val parentScope = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext.job))

        parentScope.launch { printLoop("parentTask") }
            .invokeOnCompletion { println("Completed: parentTask / $it") }

        val nestedScope = CoroutineScope(parentScope.coroutineContext + Job())
        nestedScope.launch { printLoop("nestedTask") }
            .invokeOnCompletion { println("Completed: nestedTask / $it") }

        delay(3.seconds)
        nestedScope.cancel(CancellationException("explicit"))
    }
}
