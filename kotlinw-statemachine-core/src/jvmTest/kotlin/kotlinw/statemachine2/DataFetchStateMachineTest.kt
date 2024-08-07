package kotlinw.statemachine2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class DataFetchStateMachineTest {

    @Test
    fun test() = runTest {

        val testDataSet = listOf("aaa", "abc", "bbb")
        println("Data set: $testDataSet")

        data class FilteringData(val filterFragment: String)

        val smd = DataFetchStateMachineDefinition<FilteringData, List<String>, Exception>()

        val configuredStateMachine =
            smd.configure {
                inState(smd.inProgress) { stateData ->
                    println(stateData)
                    try {
                        delay(1.seconds) // Simulate network call
                        val result = testDataSet.filter { it.startsWith(stateData.input.filterFragment) }
                        println("Result: $result")
                        smd.onReceived(result)
                    } catch (e: Exception) {
                        smd.onFailed(e)
                    }
                }

                onTransition(smd.cancel) { from, to ->
                    println("$from -> $to")
                    // TODO log
                }
            }

        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            configuredStateMachine.stateFlow.collect {
                println("State change: " + it)
            }
        }

        val executor =
            configuredStateMachine.execute {
                smd.start(FilteringData("a"))
            }

        assertEquals(smd.inProgress.name, executor.currentState.definition.name)

//            executor.dispatch { smd.cancel() }
//
//            assertEquals(smd.cancelled.name, executor.currentState.definition.name)

        delay(2.seconds)
        collector.cancel()
    }
}
