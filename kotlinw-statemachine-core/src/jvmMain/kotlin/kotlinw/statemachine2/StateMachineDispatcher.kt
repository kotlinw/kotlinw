package kotlinw.statemachine2

import kotlinw.util.coroutine.cancelAll
import kotlinw.util.coroutine.withReentrantLock
import kotlinw.util.stdlib.collection.emptyImmutableList
import kotlinw.util.stdlib.concurrent.value
import kotlinw.util.stdlib.concurrent.AtomicReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// TODO StateDataBaseType kell?
private data class InitialTransitionTargetStateDataProviderContextImpl<TransitionParameter, StateDataBaseType>(
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

sealed interface StateMachineExecutor<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    StateMachineDispatcher<StateDataBaseType, SMD>,
    StateMachineStateProvider<StateDataBaseType>,
    StateMachineStateFlowProvider<StateDataBaseType> {

    enum class Status {
        Active, Completed, Cancelled
    }

    val status: Status

    suspend fun cancel()
}

sealed interface InStateExecutionContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, StateDataType : StateDataBaseType> {

    val smd: SMD

    suspend operator fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, StateDataType, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): Nothing

    suspend operator fun <ToStateDataType : StateDataBaseType>
            NormalTransitionEventDefinition<StateDataBaseType, SMD, Unit, StateDataType, ToStateDataType>.invoke(): Nothing =
        invoke(Unit)
}

sealed interface ExecutionDefinitionContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    context(CoroutineScope)
    fun <StateDataType : StateDataBaseType> inState(
        state: NonTerminalStateDefinition<StateDataBaseType, StateDataType>,
        block: suspend InStateExecutionContext<StateDataBaseType, SMD, StateDataType>.(StateDataType) -> Unit
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
        mutableMapOf<StateDefinition<StateDataBaseType, out StateDataBaseType>, MutableList<InStateTaskDefinition<StateDataBaseType, SMD, out StateDataBaseType>>>()

    context(CoroutineScope)
    override fun <StateDataType : StateDataBaseType> inState(
        state: NonTerminalStateDefinition<StateDataBaseType, StateDataType>,
        block: suspend InStateExecutionContext<StateDataBaseType, SMD, StateDataType>.(StateDataType) -> Unit
    ) {
        inStateTasks.compute(state) { _, tasks ->
            (tasks ?: mutableListOf()).also {
                it.add(InStateTaskDefinitionImpl(this@CoroutineScope, block))
            }
        }
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
        ExecutionConfigurationImpl(inStateTasks)
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

internal sealed interface ExecutionConfiguration<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val inStateTasks: Map<StateDefinition<StateDataBaseType, out StateDataBaseType>, List<InStateTaskDefinition<StateDataBaseType, SMD, out StateDataBaseType>>>
}

sealed interface InStateTaskDefinition<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, StateDataType : StateDataBaseType> {

    val coroutineScope: CoroutineScope

    val task: suspend InStateExecutionContext<StateDataBaseType, SMD, StateDataType>.(StateDataType) -> Unit
}

data class InStateTaskDefinitionImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, StateDataType : StateDataBaseType>(
    override val coroutineScope: CoroutineScope,
    override val task: suspend InStateExecutionContext<StateDataBaseType, SMD, StateDataType>.(StateDataType) -> Unit
) :
    InStateTaskDefinition<StateDataBaseType, SMD, StateDataType>

private data class ExecutionConfigurationImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    override val inStateTasks: Map<StateDefinition<StateDataBaseType, out StateDataBaseType>, List<InStateTaskDefinition<StateDataBaseType, SMD, out StateDataBaseType>>>
) :
    ExecutionConfiguration<StateDataBaseType, SMD>

sealed interface InitialTransitionProviderContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    operator fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            InitialTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>
}

private class InitialTransitionProviderContextImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> :
    InitialTransitionProviderContext<StateDataBaseType, SMD> {

    override operator fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            InitialTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType> =
        InitialExecutableTransitionImpl(
            transitionParameter,
            targetStateDefinition,
            targetStateDataProvider
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
        val executor =
            StateMachineExecutorImpl(stateMachineDefinition, mutableStateFlow, stateFlow, executionConfiguration)
        executor.executeInitialTransition(initialTransition)
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
            NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): ToStateDataType

    suspend operator fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            NormalTransitionEventDefinition<StateDataBaseType, SMD, Unit, FromStateDataType, ToStateDataType>.invoke() {
        invoke(Unit)
    }
}

internal class StateMachineExecutorImpl<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>>(
    override val stateMachineDefinition: SMD,
    private val mutableStateFlow: MutableSharedFlow<State<StateDataBaseType, out StateDataBaseType>>,
    override val stateFlow: SharedFlow<State<StateDataBaseType, out StateDataBaseType>>,
    private val executionConfiguration: ExecutionConfiguration<StateDataBaseType, SMD>
) : StateMachineExecutor<StateDataBaseType, SMD> {

    private inner class DispatchContextImpl : DispatchContext<StateDataBaseType, SMD> {

        override val smd: SMD get() = stateMachineDefinition

        override suspend fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>.invoke(
            transitionParameter: TransitionParameter
        ) =
            executeNormalTransition(
                currentState.definition as StateDefinition<StateDataBaseType, FromStateDataType>, // TODO cast irtás?
                currentState.data as FromStateDataType, // TODO cast irtás?
                NormalExecutableTransitionImpl(transitionParameter, targetStateDefinition, targetStateDataProvider)
            )
    }

    private inner class InStateExecutionContextImpl<StateDataType : StateDataBaseType>(
        private val executedInState: State<StateDataBaseType, StateDataType>
    ) : InStateExecutionContext<StateDataBaseType, SMD, StateDataType> {

        override val smd: SMD get() = stateMachineDefinition

        override suspend fun <TransitionParameter, ToStateDataType : StateDataBaseType> NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, StateDataType, ToStateDataType>.invoke(
            transitionParameter: TransitionParameter
        ): Nothing {
            executeNormalTransition(
                executedInState.definition,
                executedInState.data,
                NormalExecutableTransitionImpl(transitionParameter, targetStateDefinition, targetStateDataProvider)
            )
            throw CancellationException()
        }
    }

    private val lock = Mutex()

    private val statusHolder = AtomicReference(StateMachineExecutor.Status.Active)

    override val status: StateMachineExecutor.Status get() = statusHolder.value

    private val currentStateHolder: AtomicReference<State<StateDataBaseType, out StateDataBaseType>?> =
        AtomicReference(null)

    private val currentStateCoroutines = AtomicReference<ImmutableList<Job>>(emptyImmutableList())

    internal suspend fun <TransitionParameter, ToStateDataType : StateDataBaseType> executeInitialTransition(
        transition: InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>
    ): ToStateDataType =
        // TODO validate transition
        executeTransition(
            stateMachineDefinition.undefined,
            transition.targetStateDefinition,
            transition.targetDataProvider(
                InitialTransitionTargetStateDataProviderContextImpl(transition.transitionParameter)
            ),
            transition.transitionParameter
        )

    internal suspend fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> executeNormalTransition(
        fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>,
        fromStateData: FromStateDataType,
        transition: NormalExecutableTransition<TransitionParameter, StateDataBaseType, FromStateDataType, ToStateDataType>
    ): ToStateDataType =
        // TODO validate transition
        executeTransition(
            fromStateDefinition,
            transition.targetStateDefinition,
            transition.targetDataProvider(
                NormalTransitionTargetStateDataProviderContextImpl(
                    fromStateDefinition,
                    fromStateData,
                    transition.transitionParameter
                )
            ),
            transition.transitionParameter
        )

    private suspend fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> executeTransition(
        fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>,
        toStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>,
        toStateData: ToStateDataType,
        transitionParameter: TransitionParameter
    ): ToStateDataType =
        lock.withReentrantLock {

            println(fromStateDefinition.name + " -> " + toStateDefinition.name) // TODO log

            currentStateCoroutines.value.apply {
                cancelAll()
                joinAll()
            }

            val newState = StateImpl(toStateDefinition, toStateData)
            currentStateHolder.value = newState
            mutableStateFlow.emit(newState)

            currentStateCoroutines.value =
                executionConfiguration.inStateTasks[toStateDefinition]?.map {
                    it.coroutineScope.launch {
                        (it.task as suspend InStateExecutionContext<StateDataBaseType, SMD, ToStateDataType>.(ToStateDataType) -> Unit)(
                            InStateExecutionContextImpl(newState),
                            toStateData
                        ) // TODO cast irtás?
                    }
                }?.toImmutableList() ?: emptyImmutableList()

            toStateData
        }

    override suspend fun <T> dispatch(block: suspend /* TODO context(SMD) */ DispatchContext<StateDataBaseType, SMD>.() -> T): T =
        lock.withReentrantLock {
            block(DispatchContextImpl())
        }

    override val currentState get() = currentStateHolder.value!!

    override suspend fun cancel() {
        lock.withReentrantLock {
            currentStateCoroutines.value.apply {
                cancelAll()
            }

            statusHolder.value = StateMachineExecutor.Status.Cancelled
        }
    }
}
