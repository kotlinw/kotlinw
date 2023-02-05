package kotlinw.statemachine2

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class StateDefinition<StateDataBaseType, StateDataType : StateDataBaseType>(
    val name: String
)

sealed interface TransitionDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> {

    val definingProperty: KProperty<*>

    val eventName: String

    val from: StateDefinition<StateDataBaseType,out  FromStateDataType>

    val to: StateDefinition<StateDataBaseType, ToStateDataType>
}

private data class TransitionDefinitionData<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>(
    override val definingProperty: KProperty<*>,
    override val eventName: String,
    override val from: StateDefinition<StateDataBaseType, out FromStateDataType>,
    override val to: StateDefinition<StateDataBaseType, ToStateDataType>
) : TransitionDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>

// TODO StateDataBaseType a FromStateDataType előtt legyen
sealed interface TransitionEventDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> {

    val name: String

    val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>

    val transitions: List<TransitionDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>>

    val targetStateDataProvider: (TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType
}

private data class TransitionEventDefinitionData<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>(
    override val name: String,
    override val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>,
    override val transitions: List<TransitionDefinitionData<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>>,
    override val targetStateDataProvider: (TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType
) : TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>

sealed interface TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType : StateDataBaseType> {

    val fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>

    val transitionParameter: TransitionParameter
}

sealed interface InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>:
    TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, Nothing>

sealed interface NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType : StateDataBaseType>:
    TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>{

    val fromStateData: FromStateDataType
}

abstract class StateMachineDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val undefined = StateDefinition<StateDataBaseType, Nothing>("undefined")

    private val _stateDefinitions = mutableListOf<StateDefinition<*, *>>(undefined)

    private val _eventDefinitions = mutableListOf<TransitionEventDefinition<StateDataBaseType, SMD, *, *, *>>()

    val states: List<StateDefinition<*, *>> get() = _stateDefinitions

    val events: List<TransitionEventDefinition<StateDataBaseType, SMD, *, *, *>> = _eventDefinitions

    // TODO why does not compile: StateDataType: StateDataBaseType
    protected fun <StateDataType : StateDataBaseType> state(): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, StateDefinition<StateDataBaseType, StateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val stateName = kProperty.name
            val stateDefinition = StateDefinition<StateDataBaseType, StateDataType>(stateName)
            _stateDefinitions.add(stateDefinition)
            ReadOnlyProperty { _, _ -> stateDefinition }
        }

    protected fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            StateDefinition<StateDataBaseType, ToStateDataType>.fromUndefined(
        provideTargetState: (InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>) -> ToStateDataType
    ): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, Nothing, ToStateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val eventName = kProperty.name
            val eventDefinition =
                TransitionEventDefinitionData<StateDataBaseType, SMD, TransitionParameter, Nothing, ToStateDataType>(
                    eventName,
                    this,
                    listOf(TransitionDefinitionData(kProperty, eventName, undefined, this)),
                    provideTargetState as (TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, Nothing>) -> ToStateDataType // TODO elkerülhető a cast?
                )
            _eventDefinitions.add(eventDefinition)
            ReadOnlyProperty { _, _ -> eventDefinition }
        }

    protected fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            StateDefinition<StateDataBaseType, ToStateDataType>.from(
        vararg fromState: StateDefinition<StateDataBaseType, out FromStateDataType>,
        provideTargetState: (NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType
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
                    provideTargetState as (TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType // TODO elkerülhető a cast?
                )
            _eventDefinitions.add(eventDefinition)
            ReadOnlyProperty { _, _ -> eventDefinition }
        }
}
