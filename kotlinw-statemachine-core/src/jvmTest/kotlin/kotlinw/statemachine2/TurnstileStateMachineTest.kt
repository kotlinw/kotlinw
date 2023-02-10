package kotlinw.statemachine2

import kotlinw.logging.mp.LoggerFactoryImpl
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TurnstileStateMachineTest {

    @Test
    fun test() {
        runBlocking {
            val loggerFactory = LoggerFactoryImpl()

            val configuredStateMachine = TurnstileStateMachineDefinition.configure(loggerFactory, this)

            val loggerJob = launch(start = CoroutineStart.UNDISPATCHED) {
                configuredStateMachine.stateFlow.collect { println("New state: ${it.definition.name}") }
            }

            val executor = configuredStateMachine.execute { smd.start() }
            assertEquals(TurnstileStateMachineDefinition.locked, executor.currentState.definition)

            // TODO `smd.` will be omitted when this issue would be fixed: https://youtrack.jetbrains.com/issue/KT-53551/suspend-functional-type-with-context-receiver-causes-ClassCastException
            executor.dispatch { smd.insertCoin() }
            assertEquals(TurnstileStateMachineDefinition.unlocked, executor.currentState.definition)

            executor.dispatch { smd.pushArm() }
            assertEquals(TurnstileStateMachineDefinition.locked, executor.currentState.definition)

            // This would throw an IllegalStateException with message: "No valid transition exists from current state 'locked' to state 'locked'."
            // executor.dispatch { smd.pushArm() }

            loggerJob.cancel()
        }
    }
}
