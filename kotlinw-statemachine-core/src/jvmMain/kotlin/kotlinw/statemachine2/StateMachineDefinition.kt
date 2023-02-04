package kotlinw.statemachine2

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StateDefinition<out StateDataType>(
    val name: String
)

sealed interface TransitionDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, out FromStateDataType, ToStateDataType> {

    val definingProperty: KProperty<*>

    val eventName: String

    val from: StateDefinition<FromStateDataType>

    val to: StateDefinition<ToStateDataType>
}

private data class TransitionDefinitionData<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, out FromStateDataType, ToStateDataType>(
    override val definingProperty: KProperty<*>,
    override val eventName: String,
    override val from: StateDefinition<FromStateDataType>,
    override val to: StateDefinition<ToStateDataType>
) : TransitionDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>

sealed interface TransitionEventDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType, ToStateDataType> {

    val name: String

    val targetStateDefinition: StateDefinition<ToStateDataType>

    val transitions: List<TransitionDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>>

    val targetStateDataProvider: (TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
}

private data class TransitionEventDefinitionData<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType, ToStateDataType>(
    override val name: String,
    override val targetStateDefinition: StateDefinition<ToStateDataType>,
    override val transitions: List<TransitionDefinitionData<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>>,
    override val targetStateDataProvider: (TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
) : TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>

sealed interface TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType> {

    val fromStateDefinition: StateDefinition<FromStateDataType>

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

    val undefined = StateDefinition<Unit>("undefined")

    private val _stateDefinitions = mutableListOf<StateDefinition<*>>(undefined)

    private val _eventDefinitions = mutableListOf<TransitionEventDefinition<StateDataBaseType, SMD, *, *, *>>()

    val states: List<StateDefinition<*>> get() = _stateDefinitions

    val events: List<TransitionEventDefinition<StateDataBaseType, SMD, *, *, *>> = _eventDefinitions

    // TODO why does not compile: StateDataType: StateDataBaseType
    protected fun <StateDataType> state(): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, StateDefinition<StateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val stateName = kProperty.name
            val stateDefinition = StateDefinition<StateDataType>(stateName)
            _stateDefinitions.add(stateDefinition)
            ReadOnlyProperty { _, _ -> stateDefinition }
        }

    protected fun <TransitionParameter, FromStateDataType, ToStateDataType>
            StateDefinition<ToStateDataType>.from(
        vararg fromState: StateDefinition<out FromStateDataType>,
        provideTargetState: (TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
    ): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val eventName = kProperty.name
            val eventDefinition =
                TransitionEventDefinitionData<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>(
                    eventName,
                    this,
                    fromState.map {
                        TransitionDefinitionData(kProperty, eventName, it, this)
                    },
                    provideTargetState
                )
            _eventDefinitions.add(eventDefinition)
            ReadOnlyProperty { _, _ -> eventDefinition }
        }
}
