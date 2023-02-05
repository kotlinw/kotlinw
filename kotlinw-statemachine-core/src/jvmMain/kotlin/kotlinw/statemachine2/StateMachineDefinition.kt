package kotlinw.statemachine2

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Public

class StateDefinition<StateDataBaseType, StateDataType : StateDataBaseType>(
    val name: String
)

sealed interface TransitionDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, ToStateDataType : StateDataBaseType> {

    val isPublic: Boolean

    val eventName: String

    val to: StateDefinition<StateDataBaseType, ToStateDataType>
}

sealed interface InitialTransitionDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, ToStateDataType : StateDataBaseType> :
    TransitionDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType> {
}

private data class InitialTransitionDefinitionImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, ToStateDataType : StateDataBaseType>(
    override val isPublic: Boolean,
    override val eventName: String,
    override val to: StateDefinition<StateDataBaseType, ToStateDataType>
) :
    InitialTransitionDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>

sealed interface NormalTransitionDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> :
    TransitionDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType> {

    val from: StateDefinition<StateDataBaseType, out FromStateDataType>
}

private data class NormalTransitionDefinitionImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>(
    override val isPublic: Boolean,
    override val eventName: String,
    override val to: StateDefinition<StateDataBaseType, ToStateDataType>,
    override val from: StateDefinition<StateDataBaseType, FromStateDataType>
) :
    NormalTransitionDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>

sealed interface TransitionEventDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> {

    val name: String

    val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>

    val transitions: List<TransitionDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>>
}

sealed interface InitialTransitionEventDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, ToStateDataType : StateDataBaseType> :
    TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, Nothing, ToStateDataType> {

    override val transitions: List<InitialTransitionDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>>

    val targetStateDataProvider: (InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>) -> ToStateDataType
}

private data class InitialTransitionEventDefinitionImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, ToStateDataType : StateDataBaseType>(
    override val name: String,
    override val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>,
    override val targetStateDataProvider: (InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>) -> ToStateDataType,
    override val transitions: List<InitialTransitionDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>>
) :
    InitialTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>

sealed interface NormalTransitionEventDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> :
    TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType> {

    override val transitions: List<NormalTransitionDefinition<StateDataBaseType, SMD, TransitionParameter, out FromStateDataType, ToStateDataType>>

    val targetStateDataProvider: (NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType
}

private data class NormalTransitionEventDefinitionImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>(
    override val name: String,
    override val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>,
    override val targetStateDataProvider: (NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType,
    override val transitions: List<NormalTransitionDefinition<StateDataBaseType, SMD, TransitionParameter, out FromStateDataType, ToStateDataType>>
) :
    NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>

sealed interface TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType> {

    val transitionParameter: TransitionParameter
}

sealed interface InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType> :
    TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>

sealed interface NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType : StateDataBaseType> :
    TransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType> {

    val fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>

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
    ): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, InitialTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val eventName = kProperty.name
            val eventDefinition =
                InitialTransitionEventDefinitionImpl<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>(
                    eventName,
                    this,
                    provideTargetState,
                    listOf(
                        InitialTransitionDefinitionImpl(
                            kProperty.visibility == KVisibility.PUBLIC,
                            eventName,
                            this
                        )
                    ),
                )
            _eventDefinitions.add(eventDefinition)
            ReadOnlyProperty { _, _ -> eventDefinition }
        }

    protected fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            StateDefinition<StateDataBaseType, ToStateDataType>.from(
        vararg fromState: StateDefinition<StateDataBaseType, out FromStateDataType>,
        provideTargetState: (NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType
    ): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val eventName = kProperty.name
            val eventDefinition =
                NormalTransitionEventDefinitionImpl<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>(
                    eventName,
                    this,
                    provideTargetState,
                    fromState.map {
                        NormalTransitionDefinitionImpl(kProperty.visibility == KVisibility.PUBLIC, eventName, this, it)
                    }
                )
            _eventDefinitions.add(eventDefinition)
            ReadOnlyProperty { _, _ -> eventDefinition }
        }

    abstract val start: InitialTransitionEventDefinition<StateDataBaseType, SMD, *, out StateDataBaseType>
}
