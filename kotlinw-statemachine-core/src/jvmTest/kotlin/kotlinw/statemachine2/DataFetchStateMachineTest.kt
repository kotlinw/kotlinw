package kotlinw.statemachine2

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class DataFetchStateMachineTest {

    @Test
    fun test() {
        runBlocking {

            // TODO runTest {}
            data class FilteringData(val filterFragment: String)

            val smd = DataFetchStateMachineDefinition<FilteringData, List<String>, Exception>()

            val configuredStateMachine = smd.configure {
                inState(inProgress) {
                    println(it)
                    try {
                        delay(100) // Simulate network call
                        val result = listOf(it.input.filterFragment)
                        println("result: $result")
                        onReceived(result)
                    } catch (e: Exception) {
                        onFailed(e)
                    }
                }

                onTransition(cancel) { from, to ->
                    println("$from -> $to")
                    // TODO log
                }
            }

            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                println("1 " + configuredStateMachine.stateFlow)
                configuredStateMachine.stateFlow.collect {
                    println("2: " + it)
                }
                println(3)
            }

            val executor = configuredStateMachine.execute {
                start(FilteringData("a"))
            }

            assertEquals(smd.inProgress.name, executor.currentState.definition.name)

//            executor.dispatch { smd.cancel() }
//
//            assertEquals(smd.cancelled.name, executor.currentState.definition.name)

            delay(2.seconds)
            collector.cancel()
        }
    }
}