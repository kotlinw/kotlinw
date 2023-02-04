package kotlinw.statemachine2

import kotlinw.util.stdlib.concurrent.value
import kotlinw.util.stdlib.concurrent.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class TransitionTargetStateDataProviderContextImpl<TransitionParameter, FromStateDataType>(
    override val fromStateDefinition: StateDefinition<FromStateDataType>,
    override val fromStateData: FromStateDataType,
    override val transitionParameter: TransitionParameter
) :
    TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>

interface State<StateDataType> {

    val definition: StateDefinition<StateDataType>

    val data: StateDataType
}

internal data class StateImpl<StateDataType>(
    override val definition: StateDefinition<StateDataType>,
    override val data: StateDataType
) : State<StateDataType>

interface StateMachineDispatcher<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val stateMachineDefinition: SMD

    suspend fun <T> dispatch(
        block: suspend /* TODO context(SMD) */ DispatchContext<StateDataBaseType, SMD>.() -> T
    ): T
}

interface StateMachineStateProvider<StateDataBaseType> {

    val currentState: State<out StateDataBaseType>
}

interface StateMachineStateFlowProvider<StateDataBaseType> {

    val stateFlow: SharedFlow<State<StateDataBaseType>>
}

interface StateMachineExecutor<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    StateMachineDispatcher<StateDataBaseType, SMD>,
    StateMachineStateProvider<StateDataBaseType>,
    StateMachineStateFlowProvider<StateDataBaseType>

sealed interface ExecutionDefinitionContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    interface InStateExecutionContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
        DispatchContext<StateDataBaseType, SMD>, StateMachineStateProvider<StateDataBaseType>

    context(CoroutineScope)
    fun <StateDataType> inState(
        state: StateDefinition<StateDataType>,
        block: suspend InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit
    )

    fun <TransitionParameter, FromStateDataType, ToStateDataType> onTransition(
        transitionEventDefinition: TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    )

    fun <FromStateDataType, ToStateDataType> onTransition(
        fromState: StateDefinition<FromStateDataType>,
        toState: StateDefinition<FromStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    )

    fun <FromStateDataType, ToStateDataType> onAnyTransition(
        block: (State<FromStateDataType>, State<ToStateDataType>) -> Unit
    )
}

private class ExecutionDefinitionContextImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    ExecutionDefinitionContext<StateDataBaseType, SMD> {

    private val inStateTasks = mutableMapOf<StateDefinition<out StateDataBaseType>, suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit>()

    context(CoroutineScope)
    override fun <StateDataType> inState(
        state: StateDefinition<StateDataType>,
        block: suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit // TODO typealias? error: bounds are not allowed for typealias
    ) {
        inStateTasks[state as StateDefinition<StateDataBaseType>] = block // TODO cast :(
    }

    override fun <TransitionParameter, FromStateDataType, ToStateDataType> onTransition(
        transitionEventDefinition: TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    ) {
        // TODO("Not yet implemented")
    }

    override fun <FromStateDataType, ToStateDataType> onTransition(
        fromState: StateDefinition<FromStateDataType>,
        toState: StateDefinition<FromStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    ) {
        // TODO("Not yet implemented")
    }

    override fun <FromStateDataType, ToStateDataType> onAnyTransition(block: (State<FromStateDataType>, State<ToStateDataType>) -> Unit) {
        // TODO("Not yet implemented")
    }

    fun build(): ExecutionConfiguration<StateDataBaseType, SMD> =
        ExecutionConfigurationImpl(emptyMap()) // TODO
}

sealed interface ExecutableTransition<TransitionParameter, FromStateDataType, ToStateDataType> {

    val targetStateDefinition: StateDefinition<ToStateDataType>

    val transitionParameter: TransitionParameter

    val targetDataProvider: (TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
}

private data class ExecutableTransitionImpl<TransitionParameter, FromStateDataType, ToStateDataType>(
    override val transitionParameter: TransitionParameter,
    override val targetStateDefinition: StateDefinition<ToStateDataType>,
    override val targetDataProvider: (TransitionTargetStateDataProviderContext<TransitionParameter, FromStateDataType>) -> ToStateDataType
) : ExecutableTransition<TransitionParameter, FromStateDataType, ToStateDataType>

sealed interface ExecutionConfiguration<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val inStateTasks: Map<State<out StateDataBaseType>, suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit>
}

private data class ExecutionConfigurationImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    override val inStateTasks: Map<State<out StateDataBaseType>, suspend ExecutionDefinitionContext.InStateExecutionContext<StateDataBaseType, SMD>.() -> Unit>
) :
    ExecutionConfiguration<StateDataBaseType, SMD>

sealed interface InitialTransitionProviderContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    operator fun <TransitionParameter, ToStateDataType>
            TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, *, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): ExecutableTransition<TransitionParameter, *, StateDataBaseType>
}

private class InitialTransitionProviderContextImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    InitialTransitionProviderContext<StateDataBaseType, SMD> {

    override operator fun <TransitionParameter, ToStateDataType>
            TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, *, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): ExecutableTransition<TransitionParameter, *, StateDataBaseType> =
        ExecutableTransitionImpl(
            transitionParameter,
            targetStateDefinition as StateDefinition<StateDataBaseType>, // TODO ez a cast elkerülhető?
            targetStateDataProvider as ((TransitionTargetStateDataProviderContext<TransitionParameter, Nothing>) -> StateDataBaseType) // TODO ez a cast elkerülhető valahogy?
        )
}

sealed interface ConfiguredStateMachine<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    StateMachineStateFlowProvider<StateDataBaseType> {

    suspend fun start(initialTransitionProvider: context(SMD) InitialTransitionProviderContext<StateDataBaseType, SMD>.() -> ExecutableTransition<*, *, StateDataBaseType>): StateMachineExecutor<StateDataBaseType, SMD>
}

private class ConfiguredStateMachineImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    private val stateMachineDefinition: SMD,
    private val executionConfiguration: ExecutionConfiguration<StateDataBaseType, SMD>
) :
    ConfiguredStateMachine<StateDataBaseType, SMD> {

    private val mutableStateFlow = MutableSharedFlow<State<StateDataBaseType>>()

    override val stateFlow = mutableStateFlow.asSharedFlow()

    override suspend fun start(initialTransitionProvider: context(SMD) InitialTransitionProviderContext<StateDataBaseType, SMD>.() -> ExecutableTransition<*, *, StateDataBaseType>): StateMachineExecutor<StateDataBaseType, SMD> {
        val initialTransition =
            initialTransitionProvider(stateMachineDefinition, InitialTransitionProviderContextImpl())
        val executor = StateMachineExecutorImpl(stateMachineDefinition, mutableStateFlow, stateFlow)
        executor.execute(stateMachineDefinition.undefined, null, initialTransition)
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

    suspend operator fun <TransitionParameter, FromStateDataType, ToStateDataType>
            TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    )

    suspend operator fun <FromStateDataType, ToStateDataType>
            TransitionEventDefinition<StateDataBaseType, SMD, Unit, FromStateDataType, ToStateDataType>.invoke() {
        invoke(Unit)
    }
}

internal class StateMachineExecutorImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    override val stateMachineDefinition: SMD,
    private val mutableStateFlow: MutableSharedFlow<State<StateDataBaseType>>,
    override val stateFlow: SharedFlow<State<StateDataBaseType>>
) : StateMachineExecutor<StateDataBaseType, SMD> {

    private inner class DispatchContextImpl : DispatchContext<StateDataBaseType, SMD> {

        override val smd: SMD get() = stateMachineDefinition

        override suspend fun <TransitionParameter, FromStateDataType, ToStateDataType> TransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>.invoke(
            transitionParameter: TransitionParameter
        ) {
            val transition =
                ExecutableTransitionImpl(transitionParameter, targetStateDefinition, targetStateDataProvider)
            execute(
                currentState.definition,
                currentState.data,
                transition as ExecutableTransition<*, *, StateDataBaseType> // TODO cast irtás
            )
        }
    }

    private val lock = Mutex()

    private val currentStateHolder: AtomicReference<State<StateDataBaseType>?> = AtomicReference(null)

    internal suspend fun execute(
        fromStateDefinition: StateDefinition<*>,
        fromStateData: Any?,
        transition: ExecutableTransition<*, *, StateDataBaseType>
    ) {
        val toStateDefinition = transition.targetStateDefinition
        // TODO validate transition

        val targetStateDataProviderContext = TransitionTargetStateDataProviderContextImpl(
            fromStateDefinition,
            fromStateData,
            transition.transitionParameter
        )
        val newStateData =
            (transition.targetDataProvider as (TransitionTargetStateDataProviderContext<Any?, Any?>) -> Any?)(
                targetStateDataProviderContext
            ) // TODO cast irtás?

        val newState = StateImpl<StateDataBaseType>(
            transition.targetStateDefinition,
            newStateData as StateDataBaseType
        ) // TODO cast irtás?

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
