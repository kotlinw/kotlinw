package statemachine2

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

class StateDefinition<StateDataType>(
    val name: String
)

class TransitionEventDefinition<FromStateDataType, ToStateDataType>()

interface TransitionTargetStateProviderContext<TransitionParameter, FromStateDataType> {

    val fromState: StateDefinition<FromStateDataType>

    val fromStateData: FromStateDataType

    val transitionParameter: TransitionParameter
}

interface TransitionDefinitionContext<TransitionParameter, ToStateDataType> {

    fun <FromStateDataType> from(
        vararg fromState: StateDefinition<out FromStateDataType>,
        block: (TransitionTargetStateProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
    )
}

abstract class StateMachineDefinition {

    protected val undefined = StateDefinition<Nothing>("undefined")

    protected fun <StateDataType> state(): PropertyDelegateProvider<StateMachineDefinition, ReadOnlyProperty<StateMachineDefinition, StateDefinition<StateDataType>>> =
        TODO()

    protected fun <TransitionParameter, ToStateDataType> transitionsTo(
        toState: StateDefinition<ToStateDataType>,
        builder: TransitionDefinitionContext<TransitionParameter, ToStateDataType>.() -> Unit
    ): PropertyDelegateProvider<StateMachineDefinition, ReadOnlyProperty<StateMachineDefinition, TransitionEventDefinition<TransitionParameter, ToStateDataType>>> =
        TODO()
}
