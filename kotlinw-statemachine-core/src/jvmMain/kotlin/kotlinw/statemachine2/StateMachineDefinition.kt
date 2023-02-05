package kotlinw.statemachine2

import kotlinw.util.stdlib.debugName
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.typeOf

sealed interface StateDefinition<StateDataBaseType, StateDataType : StateDataBaseType> {

    val name: String
}

sealed interface NonTerminalStateDefinition<StateDataBaseType, StateDataType : StateDataBaseType> :
    StateDefinition<StateDataBaseType, StateDataType>

@PublishedApi
internal data class NonTerminalStateDefinitionImpl<StateDataBaseType, StateDataType : StateDataBaseType>(
    override val name: String,
    val stateKType: KType?
) : NonTerminalStateDefinition<StateDataBaseType, StateDataType>

sealed interface TerminalStateDefinition<StateDataBaseType, StateDataType : StateDataBaseType> :
    StateDefinition<StateDataBaseType, StateDataType>

@PublishedApi
internal data class TerminalStateDefinitionImpl<StateDataBaseType, StateDataType : StateDataBaseType>(
    override val name: String,
    val stateKType: KType
) : TerminalStateDefinition<StateDataBaseType, StateDataType>

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

abstract class StateMachineDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    private val forceUniqueStateTypes: Boolean = true
) {

    val undefined: NonTerminalStateDefinition<StateDataBaseType, Nothing> =
        NonTerminalStateDefinitionImpl("undefined", null)

    @PublishedApi
    @Suppress("PropertyName")
    internal val _stateDefinitions = mutableListOf<StateDefinition<*, *>>(undefined)

    private val _eventDefinitions = mutableListOf<TransitionEventDefinition<StateDataBaseType, SMD, *, *, *>>()

    val states: List<StateDefinition<*, *>> get() = _stateDefinitions

    val events: List<TransitionEventDefinition<StateDataBaseType, SMD, *, *, *>> = _eventDefinitions

    protected inline fun <reified StateDataType : StateDataBaseType> state(): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, NonTerminalStateDefinition<StateDataBaseType, StateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val stateDefinition = NonTerminalStateDefinitionImpl<StateDataBaseType, StateDataType>(
                kProperty.name,
                typeOf<StateDataType>()
            )
            addStateDefinition(stateDefinition)
            ReadOnlyProperty { _, _ -> stateDefinition }
        }

    protected inline fun <reified StateDataType : StateDataBaseType> terminalState(): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, TerminalStateDefinition<StateDataBaseType, StateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val stateDefinition =
                TerminalStateDefinitionImpl<StateDataBaseType, StateDataType>(kProperty.name, typeOf<StateDataType>())
            addStateDefinition(stateDefinition)
            ReadOnlyProperty { _, _ -> stateDefinition }
        }

    @PublishedApi
    internal fun <StateDataType : StateDataBaseType> addStateDefinition(stateDefinition: StateDefinition<StateDataBaseType, StateDataType>) {

        fun StateDefinition<*, *>.getKType() =
            when (this) {
                is NonTerminalStateDefinitionImpl -> stateKType
                is TerminalStateDefinitionImpl -> stateKType
            }

        if (forceUniqueStateTypes) {
            val stateDefinitionKType = stateDefinition.getKType()
            _stateDefinitions.forEach {
                if (it.getKType() == stateDefinitionKType) {
                    throw IllegalStateException("${StateMachineDefinition::class.debugName} has been created with ${StateMachineDefinition<*, *>::forceUniqueStateTypes.name}=true but two states has the same type: both ${it.name} and ${stateDefinition.name} has type $stateDefinitionKType")
                }
            }
        }

        _stateDefinitions.add(stateDefinition)
    }

    protected fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            initialTransitionTo(
        targetState: StateDefinition<StateDataBaseType, ToStateDataType>,
        provideTargetState: (InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>) -> ToStateDataType
    ): PropertyDelegateProvider<SMD, ReadOnlyProperty<SMD, InitialTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>>> =
        PropertyDelegateProvider { _, kProperty ->
            val eventName = kProperty.name
            val eventDefinition =
                InitialTransitionEventDefinitionImpl<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>(
                    eventName,
                    targetState,
                    provideTargetState,
                    listOf(
                        InitialTransitionDefinitionImpl(
                            kProperty.visibility == KVisibility.PUBLIC,
                            eventName,
                            targetState
                        )
                    ),
                )
            _eventDefinitions.add(eventDefinition)
            ReadOnlyProperty { _, _ -> eventDefinition }
        }

    protected fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            StateDefinition<StateDataBaseType, ToStateDataType>.transitionFrom(
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
