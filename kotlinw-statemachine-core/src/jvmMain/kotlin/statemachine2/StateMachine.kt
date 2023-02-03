package statemachine2

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

class StateDefinition<StateDataType>(
    val name: String
)

class TransitionEventDefinition<TransitionParameter, ToStateDataType>()

interface TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType> {

    val fromState: StateDefinition<FromStateDataType>

    val fromStateData: FromStateDataType

    val transitionParameter: TransitionParameter
}

interface TransitionDefinitionContext<TransitionParameter, ToStateDataType> {

    fun <FromStateDataType> from(
        vararg fromState: StateDefinition<out FromStateDataType>,
        block: (TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
    )
}

abstract class StateMachineDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    protected val undefined = StateDefinition<Nothing>("undefined")

    protected fun <StateDataType> state(): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, StateDefinition<StateDataType>>> =
        TODO()

    protected fun <TransitionParameter, ToStateDataType> transitionsTo(
        toState: StateDefinition<ToStateDataType>,
        builder: TransitionDefinitionContext<TransitionParameter, ToStateDataType>.() -> Unit
    ): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, TransitionEventDefinition<TransitionParameter, ToStateDataType>>> =
        TODO()

    protected fun <TransitionParameter, FromStateDataType, ToStateDataType>
            StateDefinition<ToStateDataType>.from(
        vararg fromState: StateDefinition<FromStateDataType>,
        block: (TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
    ): TransitionEventDefinition<TransitionParameter, ToStateDataType> =
        TODO()
}
