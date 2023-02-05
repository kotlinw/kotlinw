package kotlinw.statemachine2

import kotlinw.util.stdlib.concurrent.value
import kotlinw.util.stdlib.concurrent.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class InitialTransitionTargetStateDataProviderContextImpl<TransitionParameter, StateDataBaseType>(
    override val fromStateDefinition: StateDefinition<StateDataBaseType, Nothing>,
    override val transitionParameter: TransitionParameter
) :
    InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>

private data class NormalTransitionTargetStateDataProviderContextImpl<TransitionParameter, StateDataBaseType, FromStateDataType : StateDataBaseType>(
    override val fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>,
    override val fromStateData: FromStateDataType,
    override val transitionParameter: TransitionParameter
) :
    NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>

interface State<StateDataBaseType, StateDataType : StateDataBaseType> {

    val definition: StateDefinition<StateDataBaseType, StateDataType>

    val data: StateDataType
}

internal data class StateImpl<StateDataBaseType, StateDataType : StateDataBaseType>(
    override val definition: StateDefinition<StateDataBaseType, StateDataType>,
    override val data: StateDataType
) : State<StateDataBaseType, StateDataType>

interface StateMachineDispatcher<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val stateMachineDefinition: SMD

    suspend fun <T> dispatch(
        block: suspend /* TODO context(SMD) */ DispatchContext<StateDataBaseType, SMD>.() -> T
    ): T
}

interface StateMachineStateProvider<StateDataBaseType> {

    val currentState: State<StateDataBaseType, out StateDataBaseType>
}

interface StateMachineStateFlowProvider<StateDataBaseType> {

    val stateFlow: SharedFlow<State<StateDataBaseType, out StateDataBaseType>>
}

interface StateMachineExecutor<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    StateMachineDispatcher<StateDataBaseType, SMD>,
    StateMachineStateProvider<StateDataBaseType>,
    StateMachineStateFlowProvider<StateDataBaseType>

sealed interface ExecutionDefinitionContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    interface InStateExecutionContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
        DispatchContext<StateDataBaseType, SMD>, StateMachineStateProvider<StateDataBaseType>

    context(CoroutineScope)
    fun <StateDataType : StateDataBaseType> inState(
        state: StateDefinition<StateDataBaseType, StateDataType>,
        block: suspend InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit
    )

    fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> onTransition(
        transitionEventDefinition: TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    )

    fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> onTransition(
        fromState: StateDefinition<StateDataBaseType, FromStateDataType>,
        toState: StateDefinition<StateDataBaseType, FromStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    )

    fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> onAnyTransition(
        block: (State<StateDataBaseType, FromStateDataType>, State<StateDataBaseType, ToStateDataType>) -> Unit
    )
}

private class ExecutionDefinitionContextImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    ExecutionDefinitionContext<StateDataBaseType, SMD> {

    private val inStateTasks =
        mutableMapOf<StateDefinition<StateDataBaseType, out StateDataBaseType>, suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit>()

    context(CoroutineScope)
    override fun <StateDataType : StateDataBaseType> inState(
        state: StateDefinition<StateDataBaseType, StateDataType>,
        block: suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit // TODO typealias? error: bounds are not allowed for typealias
    ) {
        inStateTasks[state] = block
    }

    override fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> onTransition(
        transitionEventDefinition: TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    ) {
        // TODO("Not yet implemented")
    }

    override fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> onTransition(
        fromState: StateDefinition<StateDataBaseType, FromStateDataType>,
        toState: StateDefinition<StateDataBaseType, FromStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    ) {
        // TODO("Not yet implemented")
    }

    override fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> onAnyTransition(block: (State<StateDataBaseType, FromStateDataType>, State<StateDataBaseType, ToStateDataType>) -> Unit) {
        // TODO("Not yet implemented")
    }

    fun build(): ExecutionConfiguration<StateDataBaseType, SMD> =
        ExecutionConfigurationImpl(emptyMap()) // TODO
}

sealed interface ExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType : StateDataBaseType> {

    val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>

    val transitionParameter: TransitionParameter
}

sealed interface InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType : StateDataBaseType> :
    ExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType> {

    val targetDataProvider: (InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>) -> ToStateDataType
}

private data class InitialExecutableTransitionImpl<TransitionParameter, StateDataBaseType, ToStateDataType : StateDataBaseType>(
    override val transitionParameter: TransitionParameter,
    override val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>,
    override val targetDataProvider: (InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>) -> ToStateDataType
) : InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>

sealed interface NormalExecutableTransition<TransitionParameter, StateDataBaseType, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> :
    ExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType> {

    val targetDataProvider: (NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType
}

private data class NormalExecutableTransitionImpl<TransitionParameter, StateDataBaseType, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>(
    override val transitionParameter: TransitionParameter,
    override val targetStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>,
    override val targetDataProvider: (NormalTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType, FromStateDataType>) -> ToStateDataType
) : NormalExecutableTransition<TransitionParameter, StateDataBaseType, FromStateDataType, ToStateDataType>

sealed interface ExecutionConfiguration<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val inStateTasks: Map<State<StateDataBaseType, out StateDataBaseType>, suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit>
}

private data class ExecutionConfigurationImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    override val inStateTasks: Map<State<StateDataBaseType, out StateDataBaseType>, suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit>
) :
    ExecutionConfiguration<StateDataBaseType, SMD>

sealed interface InitialTransitionProviderContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    operator fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, *, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>
}

private class InitialTransitionProviderContextImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    InitialTransitionProviderContext<StateDataBaseType, SMD> {

    override operator fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, *, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType> =
        InitialExecutableTransitionImpl(
            transitionParameter,
            targetStateDefinition,
            targetStateDataProvider as (InitialTransitionTargetStateDataProviderContext<TransitionParameter, StateDataBaseType>) -> ToStateDataType // TODO elkerülhető a cast?
        )
}

sealed interface ConfiguredStateMachine<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    StateMachineStateFlowProvider<StateDataBaseType> {

    suspend fun <ToStateDataType : StateDataBaseType> start(initialTransitionProvider: context(SMD) InitialTransitionProviderContext<StateDataBaseType, SMD>.() -> InitialExecutableTransition<*, StateDataBaseType, ToStateDataType>): StateMachineExecutor<StateDataBaseType, SMD>
}

private class ConfiguredStateMachineImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    private val stateMachineDefinition: SMD,
    private val executionConfiguration: ExecutionConfiguration<StateDataBaseType, SMD>
) :
    ConfiguredStateMachine<StateDataBaseType, SMD> {

    private val mutableStateFlow = MutableSharedFlow<State<StateDataBaseType, out StateDataBaseType>>()

    override val stateFlow = mutableStateFlow.asSharedFlow()

    override suspend fun <ToStateDataType : StateDataBaseType> start(initialTransitionProvider: context(SMD) InitialTransitionProviderContext<StateDataBaseType, SMD>.() -> InitialExecutableTransition<*, StateDataBaseType, ToStateDataType>): StateMachineExecutor<StateDataBaseType, SMD> {
        val initialTransition =
            initialTransitionProvider(stateMachineDefinition, InitialTransitionProviderContextImpl())
        val executor = StateMachineExecutorImpl(stateMachineDefinition, mutableStateFlow, stateFlow)
        executor.executeInitialTransition(stateMachineDefinition.undefined, initialTransition)
        return executor
    }
}

fun <StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> SMD.configure(
    executionDefinitionBuilder: context(SMD) ExecutionDefinitionContext<StateDataBaseType, SMD>.() -> Unit
): ConfiguredStateMachine<StateDataBaseType, SMD> =
    ConfiguredStateMachineImpl(
        this,
        ExecutionDefinitionContextImpl<StateDataBaseType, SMD>().also { executionDefinitionBuilder(this, it) }.build()
    )

sealed interface DispatchContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val smd: SMD

    suspend operator fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    )

    suspend operator fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            TransitionEventDefinition<StateDataBaseType, SMD, Unit, FromStateDataType, ToStateDataType>.invoke() {
        invoke(Unit)
    }
}

internal class StateMachineExecutorImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    override val stateMachineDefinition: SMD,
    private val mutableStateFlow: MutableSharedFlow<State<StateDataBaseType, out StateDataBaseType>>,
    override val stateFlow: SharedFlow<State<StateDataBaseType, out StateDataBaseType>>
) : StateMachineExecutor<StateDataBaseType, SMD> {

    private inner class DispatchContextImpl : DispatchContext<StateDataBaseType, SMD> {

        override val smd: SMD get() = stateMachineDefinition

        override suspend fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>.invoke(
            transitionParameter: TransitionParameter
        ) {
            val transition =
                NormalExecutableTransitionImpl(transitionParameter, targetStateDefinition, targetStateDataProvider)
            executeNormalTransition(
                currentState.definition as StateDefinition<StateDataBaseType, FromStateDataType>, // TODO cast irtás?
                currentState.data as FromStateDataType, // TODO cast irtás?
                transition
            )
        }
    }

    private val lock = Mutex()

    private val currentStateHolder: AtomicReference<State<StateDataBaseType, out StateDataBaseType>?> =
        AtomicReference(null)

    internal suspend fun <TransitionParameter, ToStateDataType : StateDataBaseType> executeInitialTransition(
        fromStateDefinition: StateDefinition<StateDataBaseType, Nothing>,
        transition: InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>
    ) {
        val toStateDefinition = transition.targetStateDefinition
        // TODO validate transition

        val targetStateDataProviderContext = InitialTransitionTargetStateDataProviderContextImpl(
            fromStateDefinition,
            transition.transitionParameter
        )
        val newStateData =
            (transition.targetDataProvider)(targetStateDataProviderContext)

        val newState = StateImpl(transition.targetStateDefinition, newStateData)

        currentStateHolder.value = newState
        println(fromStateDefinition.name + " -> " + toStateDefinition.name) // TODO log
        mutableStateFlow.emit(newState)
    }

    internal suspend fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> executeNormalTransition(
        fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>,
        fromStateData: FromStateDataType,
        transition: NormalExecutableTransition<TransitionParameter, StateDataBaseType, FromStateDataType, ToStateDataType>
    ) {
        val toStateDefinition = transition.targetStateDefinition
        // TODO validate transition

        val targetStateDataProviderContext = NormalTransitionTargetStateDataProviderContextImpl(
            fromStateDefinition,
            fromStateData,
            transition.transitionParameter
        )
        val newStateData =
            (transition.targetDataProvider)(targetStateDataProviderContext)

        val newState = StateImpl(transition.targetStateDefinition, newStateData)

        currentStateHolder.value = newState
        println(fromStateDefinition.name + " -> " + toStateDefinition.name) // TODO log
        mutableStateFlow.emit(newState)
    }

    override suspend fun <T> dispatch(block: suspend /* TODO context(SMD) */ DispatchContext<StateDataBaseType, SMD>.() -> T): T =
        lock.withLock {
            block(DispatchContextImpl())
        }

    override val currentState get() = currentStateHolder.value!!
}
