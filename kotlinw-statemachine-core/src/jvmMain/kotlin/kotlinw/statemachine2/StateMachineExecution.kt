package kotlinw.statemachine2

import arrow.atomic.Atomic
import arrow.atomic.AtomicBoolean
import arrow.atomic.value
import arrow.core.continuations.AtomicRef
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.util.coroutine.withReentrantLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue

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

interface StateMachineDispatcher<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> {

    val stateMachineDefinition: SMD

    suspend fun <T> dispatch(
        block: suspend /* TODO context(SMD) */ DispatchContext<StateDataBaseType, SMD>.(StateDataBaseType) -> T
    ): T
}

interface StateMachineStateProvider<StateDataBaseType> {

    val currentState: State<StateDataBaseType, out StateDataBaseType>
}

interface StateMachineStateFlowProvider<StateDataBaseType> {

    val stateFlow: SharedFlow<State<StateDataBaseType, out StateDataBaseType>>
}

sealed interface StateMachineExecutor<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> :
    StateMachineDispatcher<StateDataBaseType, SMD>,
    StateMachineStateProvider<StateDataBaseType>,
    StateMachineStateFlowProvider<StateDataBaseType> {

    enum class Status {
        Active, Completed, Cancelled // TODO handle Completed with terminal states
    }

    val status: Status

    suspend fun cancel()
}

sealed interface InStateExecutionContext<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>, StateDataType : StateDataBaseType> {

    val smd: SMD

    suspend operator fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, StateDataType, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): Nothing

    suspend operator fun <ToStateDataType : StateDataBaseType>
            NormalTransitionEventDefinition<StateDataBaseType, SMD, Unit, StateDataType, ToStateDataType>.invoke(): Nothing =
        invoke(Unit)

    // TODO forbid state change from the finalizer
    suspend fun onBeforeStateChange(finalizer: StateFinalizerTask<StateDataType>)

    // TODO forbid state change from the finalizer
    suspend fun onCancellation(finalizer: StateFinalizerTask<StateDataType>)
}

typealias InStateTask<StateDataBaseType, SMD, StateDataType> = suspend InStateExecutionContext<StateDataBaseType, SMD, StateDataType>.(StateDataType) -> Unit

sealed interface ExecutionDefinitionContext<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> {

    val smd: SMD

    /* TODO context(CoroutineScope) */
    fun <StateDataType : StateDataBaseType> inState(
        state: NonTerminalStateDefinition<StateDataBaseType, StateDataType>,
        block: InStateTask<StateDataBaseType, SMD, StateDataType>
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

private class ExecutionDefinitionContextImpl<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>>(
    override val smd: SMD
) :
    ExecutionDefinitionContext<StateDataBaseType, SMD> {

    private val inStateTasks =
        mutableMapOf<StateDefinition<StateDataBaseType, out StateDataBaseType>, InStateTaskDefinition<StateDataBaseType, SMD, out StateDataBaseType>>()

    /* TODO context(CoroutineScope) */
    override fun <StateDataType : StateDataBaseType> inState(
        state: NonTerminalStateDefinition<StateDataBaseType, StateDataType>,
        block: InStateTask<StateDataBaseType, SMD, StateDataType>
    ) {
        check(!inStateTasks.containsKey(state))
        inStateTasks[state] = InStateTaskDefinitionImpl(block)
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

internal sealed interface ExecutionConfiguration<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> {

    val inStateTasks: Map<StateDefinition<StateDataBaseType, out StateDataBaseType>, InStateTaskDefinition<StateDataBaseType, SMD, out StateDataBaseType>>
}

sealed interface InStateTaskDefinition<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>, StateDataType : StateDataBaseType> {

    val task: InStateTask<StateDataBaseType, SMD, StateDataType>
}

data class InStateTaskDefinitionImpl<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>, StateDataType : StateDataBaseType>(
    override val task: InStateTask<StateDataBaseType, SMD, StateDataType>
) :
    InStateTaskDefinition<StateDataBaseType, SMD, StateDataType>

private data class ExecutionConfigurationImpl<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>>(
    override val inStateTasks: Map<StateDefinition<StateDataBaseType, out StateDataBaseType>, InStateTaskDefinition<StateDataBaseType, SMD, out StateDataBaseType>>
) :
    ExecutionConfiguration<StateDataBaseType, SMD>

sealed interface InitialTransitionProviderContext<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> {

    val smd: SMD

    operator fun <TransitionParameter, ToStateDataType : StateDataBaseType>
            InitialTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>

    operator fun <ToStateDataType : StateDataBaseType>
            InitialTransitionEventDefinition<StateDataBaseType, SMD, Unit, ToStateDataType>.invoke(): InitialExecutableTransition<Unit, StateDataBaseType, ToStateDataType> =
        invoke(Unit)
}

private class InitialTransitionProviderContextImpl<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>>(
    override val smd: SMD
) :
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

sealed interface ConfiguredStateMachine<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> :
    StateMachineStateFlowProvider<StateDataBaseType> {

    context(CoroutineScope)
    suspend fun <ToStateDataType : StateDataBaseType> execute(initialTransitionProvider: /* TODO context(SMD) */ InitialTransitionProviderContext<StateDataBaseType, SMD>.() -> InitialExecutableTransition<*, StateDataBaseType, ToStateDataType>): StateMachineExecutor<StateDataBaseType, SMD>
}

private class ConfiguredStateMachineImpl<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>>(
    private val stateMachineDefinition: SMD,
    private val executionConfiguration: ExecutionConfiguration<StateDataBaseType, SMD>
) :
    ConfiguredStateMachine<StateDataBaseType, SMD> {

    private val mutableStateFlow = MutableSharedFlow<State<StateDataBaseType, out StateDataBaseType>>()

    override val stateFlow = mutableStateFlow.asSharedFlow()

    context(CoroutineScope)
    override suspend fun <ToStateDataType : StateDataBaseType> execute(initialTransitionProvider: /* context(SMD) */ InitialTransitionProviderContext<StateDataBaseType, SMD>.() -> InitialExecutableTransition<*, StateDataBaseType, ToStateDataType>): StateMachineExecutor<StateDataBaseType, SMD> {
        val initialTransition =
            initialTransitionProvider(InitialTransitionProviderContextImpl(stateMachineDefinition))
        val executor =
            StateMachineExecutorImpl(
                this@CoroutineScope,
                stateMachineDefinition,
                mutableStateFlow,
                stateFlow,
                executionConfiguration
            )
        executor.executeInitialTransition(initialTransition)
        return executor
    }
}

fun <StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> SMD.configure(
    executionDefinitionBuilder: /* TODO context(SMD) */ ExecutionDefinitionContext<StateDataBaseType, SMD>.() -> Unit = {}
): ConfiguredStateMachine<StateDataBaseType, SMD> =
    ConfiguredStateMachineImpl(
        this,
        ExecutionDefinitionContextImpl<StateDataBaseType, SMD>(this).also {
            executionDefinitionBuilder(
                it
            )
        }.build()
    )

sealed interface DispatchContext<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> {

    val smd: SMD

    val currentState: State<StateDataBaseType, out StateDataBaseType>

    suspend operator fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, FromStateDataType, ToStateDataType>.invoke(
        transitionParameter: TransitionParameter
    ): ToStateDataType

    suspend operator fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType>
            NormalTransitionEventDefinition<StateDataBaseType, SMD, Unit, FromStateDataType, ToStateDataType>.invoke() {
        invoke(Unit)
    }
}

typealias StateFinalizerTask<T> = suspend (T) -> Unit

internal class StateMachineExecutorImpl<StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>>(
    private val coroutineScope: CoroutineScope,
    override val stateMachineDefinition: SMD,
    private val mutableStateFlow: MutableSharedFlow<State<StateDataBaseType, out StateDataBaseType>>,
    override val stateFlow: SharedFlow<State<StateDataBaseType, out StateDataBaseType>>,
    private val executionConfiguration: ExecutionConfiguration<StateDataBaseType, SMD>
) : StateMachineExecutor<StateDataBaseType, SMD> {

    private val logger = PlatformLogging.getLogger()

    private inner class DispatchContextImpl : DispatchContext<StateDataBaseType, SMD> {

        override val smd: SMD get() = stateMachineDefinition

        override val currentState get() = this@StateMachineExecutorImpl.currentState

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

        val cancellationByStateChange = AtomicBoolean(false)

        private val onBeforeStateChangeFinalizerTasks = ConcurrentLinkedQueue<StateFinalizerTask<StateDataType>>()

        private val onCancellationFinalizerTasks = ConcurrentLinkedQueue<StateFinalizerTask<StateDataType>>()

        override suspend fun <TransitionParameter, ToStateDataType : StateDataBaseType> NormalTransitionEventDefinition<StateDataBaseType, SMD, TransitionParameter, StateDataType, ToStateDataType>.invoke(
            transitionParameter: TransitionParameter
        ): Nothing {
            executeNormalTransition(
                executedInState.definition,
                executedInState.data,
                NormalExecutableTransitionImpl(transitionParameter, targetStateDefinition, targetStateDataProvider)
            )
            check(currentCoroutineContext().job.isCancelled) // It should have been cancelled by the above executeNormalTransition() call
            throw CancellationException()
        }

        override suspend fun onBeforeStateChange(finalizer: StateFinalizerTask<StateDataType>) {
            onBeforeStateChangeFinalizerTasks.add(finalizer)
        }

        override suspend fun onCancellation(finalizer: StateFinalizerTask<StateDataType>) {
            onCancellationFinalizerTasks.add(finalizer)
        }

        private suspend fun runFinalizerTasks(
            tasks: Iterable<StateFinalizerTask<StateDataType>>,
            errorLoggingDiscriminator: String
        ) {
            withContext(NonCancellable) {
                tasks.forEach {
                    try {
                        it(executedInState.data)
                    } catch (e: Exception) {
                        logger.error(e) { "$errorLoggingDiscriminator task failed." }
                    }
                }
            }
        }

        suspend fun runFinalizersBeforeStateChange() {
            runFinalizerTasks(onBeforeStateChangeFinalizerTasks, this::onBeforeStateChange.name)
        }

        suspend fun runFinalizersOnCancellation() {
            runFinalizerTasks(onCancellationFinalizerTasks, this::onCancellation.name)
        }
    }

    private val lock = Mutex()

    private val statusHolder = Atomic(StateMachineExecutor.Status.Active)

    override val status: StateMachineExecutor.Status get() = statusHolder.value

    private val currentStateHolder: AtomicRef<State<StateDataBaseType, out StateDataBaseType>?> =
        AtomicRef(null)

    private inner class InStateExecutionContextData(val context: InStateExecutionContextImpl<*>, val job: Job)

    private val currentStateExecutionContextHolder = AtomicRef<InStateExecutionContextData?>(null)

    internal suspend fun <TransitionParameter, ToStateDataType : StateDataBaseType> executeInitialTransition(
        transition: InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>
    ): ToStateDataType =
        lock.withReentrantLock {
            validateInitialTransition(transition)
            executeTransition(
                stateMachineDefinition.undefined,
                transition.targetStateDefinition,
                transition.targetDataProvider(
                    InitialTransitionTargetStateDataProviderContextImpl(transition.transitionParameter)
                )
            )
        }

    private fun <TransitionParameter, ToStateDataType : StateDataBaseType> validateInitialTransition(transition: InitialExecutableTransition<TransitionParameter, StateDataBaseType, ToStateDataType>) {
        check(
            stateMachineDefinition.events
                .filterIsInstance<InitialTransitionEventDefinition<*, *, *, *>>()
                .any { event ->
                    event.transitions.any { it.to == transition.targetStateDefinition }
                }
        ) {
            "No valid initial transition exists to state '${transition.targetStateDefinition.name}'."
        }
    }

    internal suspend fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> executeNormalTransition(
        fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>,
        fromStateData: FromStateDataType,
        transition: NormalExecutableTransition<TransitionParameter, StateDataBaseType, FromStateDataType, ToStateDataType>
    ): ToStateDataType =
        lock.withReentrantLock {
            validateNormalTransition(transition)
            executeTransition(
                fromStateDefinition,
                transition.targetStateDefinition,
                transition.targetDataProvider(
                    NormalTransitionTargetStateDataProviderContextImpl(
                        fromStateDefinition,
                        fromStateData,
                        transition.transitionParameter
                    )
                )
            )
        }

    private fun <TransitionParameter, FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> validateNormalTransition(
        transition: NormalExecutableTransition<TransitionParameter, StateDataBaseType, FromStateDataType, ToStateDataType>
    ) {
        val currentStateDefinition = currentState.definition
        check(
            stateMachineDefinition.events
                .filterIsInstance<NormalTransitionEventDefinition<*, *, *, *, *>>()
                .any { event ->
                    event.transitions.any { it.from == currentStateDefinition && it.to == transition.targetStateDefinition }
                }
        ) {
            "No valid transition exists from current state '${currentStateDefinition.name}' to state '${transition.targetStateDefinition.name}'."
        }
    }

    private suspend fun <FromStateDataType : StateDataBaseType, ToStateDataType : StateDataBaseType> executeTransition(
        fromStateDefinition: StateDefinition<StateDataBaseType, FromStateDataType>,
        toStateDefinition: StateDefinition<StateDataBaseType, ToStateDataType>,
        toStateData: ToStateDataType
    ): ToStateDataType =
        lock.withReentrantLock {

            logger.debug { "State change: " / fromStateDefinition.name / " -> " / toStateDefinition.name }

            withContext(NonCancellable) {
                currentStateExecutionContextHolder.value?.also {
                    val context = it.context

                    context.cancellationByStateChange.value = true
                    it.job.cancelAndJoin()

                    context.runFinalizersBeforeStateChange()
                }

                currentStateExecutionContextHolder.value = null
            }

            val newState = StateImpl(toStateDefinition, toStateData)
            currentStateHolder.value = newState
            mutableStateFlow.emit(newState)

            executionConfiguration.inStateTasks[toStateDefinition]?.also { inStateTaskDefinition ->
                val context = InStateExecutionContextImpl(newState)
                val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        (inStateTaskDefinition.task as InStateTask<StateDataBaseType, SMD, ToStateDataType>)(
                            context,
                            toStateData
                        ) // TODO cast irtás?
                    } catch (e: CancellationException) {
                        if (!context.cancellationByStateChange.value) {
                            context.runFinalizersOnCancellation()
                        }
                        throw e
                    }
                }
                currentStateExecutionContextHolder.value = InStateExecutionContextData(context, job)
                job.start()
            }

            toStateData
        }

    override suspend fun <T> dispatch(block: suspend /* TODO context(SMD) */ DispatchContext<StateDataBaseType, SMD>.(StateDataBaseType) -> T): T =
        lock.withReentrantLock {
            withContext(NonCancellable) {
                block(DispatchContextImpl(), currentStateHolder.value!!.data)
            }
        }

    override val currentState get() = currentStateHolder.value!!

    override suspend fun cancel() {
        lock.withReentrantLock {
            currentStateExecutionContextHolder.value?.also {
                withContext(NonCancellable) {
                    currentStateExecutionContextHolder.value?.also {
                        it.job.cancelAndJoin()
                        currentStateExecutionContextHolder.value = null
                    }

                    statusHolder.value = StateMachineExecutor.Status.Cancelled
                }
            }
        }
    }
}
