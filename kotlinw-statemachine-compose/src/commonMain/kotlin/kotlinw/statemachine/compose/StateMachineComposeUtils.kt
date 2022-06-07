package kotlinw.statemachine.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinw.statemachine.MutableStateMachine
import kotlinw.statemachine.State
import kotlinw.statemachine.StateDefinition
import kotlinw.statemachine.StateMachineDefinition
import kotlinw.statemachine.StateMachineSnapshot
import kotlinw.statemachine.StateMachineTransitionDispatcher

class StateMatcherContext<SMDefinition : StateMachineDefinition<SMDefinition>>(
    val definition: SMDefinition,
    private val currentState: State<SMDefinition, *>
) {
    private val _executedMatchers = mutableListOf<StateDefinition<SMDefinition, *>>()

    internal val executedMatchers: List<StateDefinition<SMDefinition, *>> get() = _executedMatchers

    @Composable
    internal fun <T> onState(state: StateDefinition<SMDefinition, T>, onMatch: @Composable (T) -> Unit) {
        _executedMatchers.add(state)
        if (state == currentState.definition) {
            onMatch(currentState.data as T)
        }
    }
}

@Composable
fun <SMDefinition : StateMachineDefinition<SMDefinition>, StateDataType> StateMatcherContext<SMDefinition>.on(
    state: StateDefinition<SMDefinition, StateDataType>,
    onMatch: @Composable (StateDataType) -> Unit
) {
    onState(state, onMatch)
}

@Composable
fun <SMDefinition : StateMachineDefinition<SMDefinition>> StateMachineSnapshot<SMDefinition>.match(
    forceCompleteMatching: Boolean = true,
    matchers: @Composable StateMatcherContext<SMDefinition>.() -> Unit
) {
    check(currentState != definition.undefined) { "State machine's current state is undefined." }

    StateMatcherContext(definition, currentState).also { context ->
        context.matchers()

        if (forceCompleteMatching) {
            val allDefinedStates = definition.states.toSet() - definition.undefined
            check(context.executedMatchers.containsAll(allDefinedStates)) {
                "Incomplete state matching detected, the following states were not checked for match: ${(allDefinedStates.toSet() - context.executedMatchers.toSet()).joinToString { it.name }}"
            }
        }
    }
}

data class StateMachineState<SMDefinition : StateMachineDefinition<SMDefinition>>(
    val currentState: State<SMDefinition, Any?>,
    val dispatcher: StateMachineTransitionDispatcher<SMDefinition>
)

@Composable
fun <SMDefinition : StateMachineDefinition<SMDefinition>> rememberStateMachineState(
    stateMachineDefinition: SMDefinition,
    key1: Any?,
    initializer: StateMachineTransitionDispatcher<SMDefinition>.() -> Unit
): androidx.compose.runtime.State<StateMachineState<SMDefinition>> =
    rememberStateMachineState(stateMachineDefinition, listOf(key1), initializer)

@Composable
fun <SMDefinition : StateMachineDefinition<SMDefinition>> rememberStateMachineState(
    stateMachineDefinition: SMDefinition,
    vararg keys: Any?,
    initializer: StateMachineTransitionDispatcher<SMDefinition>.() -> Unit
): androidx.compose.runtime.State<StateMachineState<SMDefinition>> =
    rememberStateMachineState(stateMachineDefinition, keys.toList(), initializer)

@Composable
private fun <SMDefinition : StateMachineDefinition<SMDefinition>> rememberStateMachineState(
    stateMachineDefinition: SMDefinition,
    keys: List<Any?>,
    initializer: StateMachineTransitionDispatcher<SMDefinition>.() -> Unit
): androidx.compose.runtime.State<StateMachineState<SMDefinition>> {
    val stateMachine = remember(keys) { MutableStateMachine(stateMachineDefinition).also { it.initializer() } }
    val stateMachineState by stateMachine.stateFlow.collectAsState(stateMachine.stateFlow.value)
    return derivedStateOf {
        StateMachineState(
            stateMachineState.currentState,
            stateMachine
        )
    }
}
