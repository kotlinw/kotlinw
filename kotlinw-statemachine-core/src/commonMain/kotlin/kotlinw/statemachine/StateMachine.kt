package kotlinw.statemachine

import kotlinw.util.AtomicReference
import kotlinw.util.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

sealed interface State<SMDefinition : StateMachineDefinition<SMDefinition>, out StateDataType> {
    val data: StateDataType

    val definition: StateDefinition<SMDefinition, StateDataType>
}

private data class StateImpl<SMDefinition : StateMachineDefinition<SMDefinition>, StateDataType>(
    override val data: StateDataType,
    override val definition: StateDefinition<SMDefinition, StateDataType>
) : State<SMDefinition, StateDataType> {
    val name: String get() = definition.name

    override fun toString(): String {
        return "StateImpl(name='$name', data=$data)"
    }
}

data class TransitionDefinition<SMDefinition : StateMachineDefinition<SMDefinition>, SourceStateDataType, TargetStateDataType, ParameterType>(
    val definingPropertyName: String,
    val eventName: String,
    val from: StateDefinition<SMDefinition, SourceStateDataType>,
    val to: StateDefinition<SMDefinition, TargetStateDataType>,
    val condition: (TransitionContext<SourceStateDataType, ParameterType, SMDefinition>) -> Boolean,
    val action: (TransitionContext<SourceStateDataType, ParameterType, SMDefinition>) -> TargetStateDataType
)

data class TransitionEventDefinition<SMDefinition : StateMachineDefinition<SMDefinition>>(
    val name: String,
    val transitions: List<TransitionDefinition<SMDefinition, *, *, *>>
)

abstract class TransitionsDefinitionBuilder<SMDefinition : StateMachineDefinition<SMDefinition>, ParameterType> {
    protected val _transitions =
        mutableListOf<TransitionDefinition<SMDefinition, *, *, ParameterType>>()

    val transitions get(): List<TransitionDefinition<SMDefinition, *, *, ParameterType>> = _transitions

    abstract val definingPropertyName: String
}

class TransitionsToDefinitionBuilder<SMDefinition : StateMachineDefinition<SMDefinition>, TargetStateDataType, ParameterType>(
    private val to: StateDefinition<SMDefinition, TargetStateDataType>,
    override val definingPropertyName: String,
    private val eventName: String = definingPropertyName
) : TransitionsDefinitionBuilder<SMDefinition, ParameterType>() {
    fun <SourceStateDataType> from(
        from: StateDefinition<SMDefinition, SourceStateDataType>,
        condition: (TransitionContext<SourceStateDataType, ParameterType, SMDefinition>) -> Boolean = { true },
        action: TransitionContext<SourceStateDataType, ParameterType, SMDefinition>.() -> TargetStateDataType
    ) {
        _transitions.add(TransitionDefinition(definingPropertyName, eventName, from, to, condition, action))
    }
}

class TransitionsFromDefinitionBuilder<SMDefinition : StateMachineDefinition<SMDefinition>, SourceStateDataType, ParameterType>(
    private val from: StateDefinition<SMDefinition, SourceStateDataType>,
    override val definingPropertyName: String,
    private val eventName: String = definingPropertyName
) : TransitionsDefinitionBuilder<SMDefinition, ParameterType>() {
    fun <TargetStateDataType> to(
        to: StateDefinition<SMDefinition, TargetStateDataType>,
        condition: (TransitionContext<SourceStateDataType, ParameterType, SMDefinition>) -> Boolean = { true },
        action: TransitionContext<SourceStateDataType, ParameterType, SMDefinition>.() -> TargetStateDataType
    ) {
        _transitions.add(TransitionDefinition(definingPropertyName, eventName, from, to, condition, action))
    }
}

class GenericTransitionsDefinitionBuilder<SMDefinition : StateMachineDefinition<SMDefinition>, ParameterType>(
    override val definingPropertyName: String,
    private val eventName: String = definingPropertyName
) : TransitionsDefinitionBuilder<SMDefinition, ParameterType>() {
    fun <SourceStateDataType, TargetStateDataType> transition(
        from: StateDefinition<SMDefinition, SourceStateDataType>,
        to: StateDefinition<SMDefinition, TargetStateDataType>,
        condition: (TransitionContext<SourceStateDataType, ParameterType, SMDefinition>) -> Boolean = { true },
        action: TransitionContext<SourceStateDataType, ParameterType, SMDefinition>.() -> TargetStateDataType
    ) {
        _transitions.add(TransitionDefinition(definingPropertyName, eventName, from, to, condition, action))
    }
}

interface StateMachineTransitionDispatcher<SMDefinition : StateMachineDefinition<SMDefinition>> {
    val definition: SMDefinition

    fun <ParameterType> dispatch(
        executor: TransitionExecutor<SMDefinition, ParameterType>,
        parameter: ParameterType
    )

    fun dispatch(executor: TransitionExecutor<SMDefinition, Unit>) = dispatch(executor, Unit)
}

interface TransitionExecutor<SMDefinition : StateMachineDefinition<SMDefinition>, ParameterType> {
    fun execute(
        stateMachineSnapshot: StateMachineSnapshot<SMDefinition>,
        parameter: ParameterType,
        dispatcher: StateMachineTransitionDispatcher<SMDefinition>
    ): StateMachineSnapshot<SMDefinition>
}

data class TransitionContext<FromType, ParameterType, SMDefinition : StateMachineDefinition<SMDefinition>>(
    val from: FromType,
    val parameter: ParameterType,
    private val dispatcher: StateMachineTransitionDispatcher<SMDefinition>
) : StateMachineTransitionDispatcher<SMDefinition> by dispatcher

data class StateDefinition<SMDefinition : StateMachineDefinition<SMDefinition>, out StateDataType>(
    val name: String,
    val stateMachineDefinition: SMDefinition
)

abstract class StateMachineDefinition<SMDefinition : StateMachineDefinition<SMDefinition>> {
    val undefined: StateDefinition<SMDefinition, Unit> =
        StateDefinition(StateMachineDefinition<SMDefinition>::undefined.name, this as SMDefinition)

    private val _stateDefinitions = mutableListOf<StateDefinition<SMDefinition, *>>(undefined)

    private val _eventDefinitions = mutableListOf<TransitionEventDefinition<SMDefinition>>()

    protected fun <StateDataType> state() =
        PropertyDelegateProvider<SMDefinition, ReadOnlyProperty<SMDefinition, StateDefinition<SMDefinition, StateDataType>>> { _, kProperty ->
            val stateName = kProperty.name
            val stateDefinition = StateDefinition<SMDefinition, StateDataType>(stateName, this as SMDefinition)
            _stateDefinitions.add(stateDefinition)
            ReadOnlyProperty { _, _ -> stateDefinition }
        }

    protected fun <TargetStateDataType, ParameterType> transitionsTo(
        to: StateDefinition<SMDefinition, TargetStateDataType>,
        define: TransitionsToDefinitionBuilder<SMDefinition, TargetStateDataType, ParameterType>.() -> Unit
    ) =
        PropertyDelegateProvider<SMDefinition, ReadOnlyProperty<SMDefinition, TransitionExecutor<SMDefinition, ParameterType>>> { _, kProperty ->
            val eventName = kProperty.name
            val transitions = TransitionsToDefinitionBuilder<SMDefinition, TargetStateDataType, ParameterType>(
                to,
                eventName
            ).also { it.define() }.transitions
            transitionsPropertyImplementation(eventName, transitions)
        }

    protected fun <SourceStateDataType, ParameterType> transitionsFrom(
        from: StateDefinition<SMDefinition, SourceStateDataType>,
        define: TransitionsFromDefinitionBuilder<SMDefinition, SourceStateDataType, ParameterType>.() -> Unit
    ) =
        PropertyDelegateProvider<SMDefinition, ReadOnlyProperty<SMDefinition, TransitionExecutor<SMDefinition, ParameterType>>> { _, kProperty ->
            val eventName = kProperty.name
            val transitions = TransitionsFromDefinitionBuilder<SMDefinition, SourceStateDataType, ParameterType>(
                from,
                eventName
            ).also { it.define() }.transitions
            transitionsPropertyImplementation(eventName, transitions)
        }

    protected fun <ParameterType> transitions(
        define: GenericTransitionsDefinitionBuilder<SMDefinition, ParameterType>.() -> Unit
    ) =
        PropertyDelegateProvider<SMDefinition, ReadOnlyProperty<SMDefinition, TransitionExecutor<SMDefinition, ParameterType>>> { _, kProperty ->
            val eventName = kProperty.name
            val transitions =
                GenericTransitionsDefinitionBuilder<SMDefinition, ParameterType>(eventName).also { it.define() }.transitions
            transitionsPropertyImplementation(eventName, transitions)
        }

    protected fun <SourceStateDataType, TargetStateDataType, ParameterType> transition(
        from: StateDefinition<SMDefinition, SourceStateDataType>,
        to: StateDefinition<SMDefinition, TargetStateDataType>,
        condition: (TransitionContext<SourceStateDataType, ParameterType, SMDefinition>) -> Boolean = { true },
        action: TransitionContext<SourceStateDataType, ParameterType, SMDefinition>.() -> TargetStateDataType
    ) = transitions<ParameterType> {
        transition(from, to, condition, action)
    }

    private fun <ParameterType> transitionsPropertyImplementation(
        eventName: String,
        transitions: List<TransitionDefinition<SMDefinition, *, *, ParameterType>>
    ): ReadOnlyProperty<SMDefinition, TransitionExecutor<SMDefinition, ParameterType>> {
        _eventDefinitions.add(TransitionEventDefinition(eventName, transitions))
        return ReadOnlyProperty { _, _ ->
            object : TransitionExecutor<SMDefinition, ParameterType> {
                override fun execute(
                    stateMachineSnapshot: StateMachineSnapshot<SMDefinition>,
                    parameter: ParameterType,
                    dispatcher: StateMachineTransitionDispatcher<SMDefinition>
                ): StateMachineSnapshot<SMDefinition> {
                    val currentState = stateMachineSnapshot.currentState
                    val currentData = currentState.data
                    for (transition in transitions) {
                        if (currentState.definition == transition.from) {
                            val condition =
                                transition.condition as (TransitionContext<Any?, ParameterType, SMDefinition>) -> Boolean
                            // TODO kezelni, ha transition.from != currentState
                            val transitionContext = TransitionContext(currentData, parameter, dispatcher)
                            if (condition.invoke(transitionContext)) {
                                val action =
                                    transition.action as (TransitionContext<Any?, ParameterType, SMDefinition>) -> Any?
                                val newState = StateImpl(action(transitionContext), transition.to)
                                return stateMachineSnapshot.copy(currentState = newState)
                            }
                        }
                    }

                    throw IllegalStateException("currentState=$currentState")
                }
            }
        }
    }

    val states: List<StateDefinition<SMDefinition, *>> get() = _stateDefinitions

    val events: List<TransitionEventDefinition<SMDefinition>> = _eventDefinitions

    override fun toString() = this::class.simpleName!!
}

data class StateMachineSnapshot<SMDefinition : StateMachineDefinition<SMDefinition>>(
    val definition: SMDefinition,
    val currentState: State<SMDefinition, *> = StateImpl(Unit, definition.undefined)
) {
    override fun toString() =
        "StateMachine(definitionName=${definition::class.simpleName}, currentStateName=${currentState.definition.name})"
}

/**
 * Non-thread-safe state machine dispatcher.
 */
fun <SMDefinition : StateMachineDefinition<SMDefinition>, ParameterType : Any?> StateMachineSnapshot<SMDefinition>.dispatch(
    executor: TransitionExecutor<SMDefinition, ParameterType>,
    parameter: ParameterType
): StateMachineSnapshot<SMDefinition> {
    val resultSnapshot = AtomicReference(this)

    val dispatcher = object : StateMachineTransitionDispatcher<SMDefinition> {
        override val definition: SMDefinition get() = this@dispatch.definition

        override fun <ParameterType> dispatch(
            executor: TransitionExecutor<SMDefinition, ParameterType>,
            parameter: ParameterType
        ) {
            resultSnapshot.value = executor.execute(resultSnapshot.value, parameter, this)
        }
    }

    dispatcher.dispatch(executor, parameter)

    return resultSnapshot.value
}

/**
 * Non-thread-safe state machine dispatcher.
 */
fun <SMDefinition : StateMachineDefinition<SMDefinition>> StateMachineSnapshot<SMDefinition>.dispatch(
    executor: TransitionExecutor<SMDefinition, Unit>
): StateMachineSnapshot<SMDefinition> =
    dispatch(executor, Unit)

/**
 * Thread-safe mutable state machine implementation.
 */
class MutableStateMachine<SMDefinition : StateMachineDefinition<SMDefinition>>(
    override val definition: SMDefinition
) : StateMachineTransitionDispatcher<SMDefinition> {
    private val _stateFlow = MutableStateFlow(StateMachineSnapshot(definition))

    val stateFlow: StateFlow<StateMachineSnapshot<SMDefinition>> get() = _stateFlow

    val currentSnapshot get() = _stateFlow.value

    // TODO HOMEAUT-105: private val lock = Lock()

    override fun <ParameterType : Any?> dispatch(
        executor: TransitionExecutor<SMDefinition, ParameterType>,
        parameter: ParameterType
    ) {
        val currentState = currentSnapshot.currentState
        // TODO HOMEAUT-105: lock.withLock {
            val lockedSnapshot = currentSnapshot
            check(currentState == lockedSnapshot.currentState)
            _stateFlow.update {
                executor.execute(lockedSnapshot, parameter, this)
            }
//        }
    }
}

suspend fun <SMDefinition : StateMachineDefinition<SMDefinition>, StateDataType> MutableStateMachine<SMDefinition>.awaitState(
    coroutineScope: CoroutineScope,
    stateDefinition: StateDefinition<SMDefinition, StateDataType>
) {
    if (currentSnapshot.currentState != stateDefinition) {
        coroutineScope.launch {
            stateFlow.collect {
                if (it.currentState.definition == stateDefinition) {
                    currentCoroutineContext().cancel()
                }
            }
        }.join()
    }
}
