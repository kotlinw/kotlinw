package statemachine2

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

sealed interface DataFetchStatus<InputType, DataType, ErrorType> {

    val input: InputType

    data class InProgress<InputType>(
        override val input: InputType,
        val cancel: () -> Unit
    ) : DataFetchStatus<InputType, Unit, Unit>

    data class Received<InputType, DataType>(
        override val input: InputType,
        val data: DataType
    ) : DataFetchStatus<InputType, DataType, Unit>

    data class Cancelled<InputType>(
        override val input: InputType
    ) : DataFetchStatus<InputType, Unit, Unit>

    data class Failed<InputType, ErrorType>(
        override val input: InputType,
        val error: ErrorType
    ) : DataFetchStatus<InputType, Unit, ErrorType>
}

class DataFetchStateMachineDefinition<InputType, DataType, ErrorType> :
    StateMachineDefinition() {

    val inProgress by state<DataFetchStatus.InProgress<InputType>>()

    val received by state<DataFetchStatus.Received<InputType, DataType>>()

    val cancelled by state<DataFetchStatus.Cancelled<InputType>>()

    val failed by state<DataFetchStatus.Failed<InputType, ErrorType>>()

    // TODO val cancel = inProgress transitionTo cancelled {}

    val cancel by transitionsTo<Nothing, _>(cancelled) {
        from(inProgress) {
            DataFetchStatus.Cancelled(it.fromStateData.input)
        }
    }

    internal val onReceived by transitionsTo<DataType, _>(received) {
        from(inProgress) {
            DataFetchStatus.Received(it.fromStateData.input, it.transitionParameter)
        }
    }

    internal val onFailed by transitionsTo(failed) {
        from(inProgress) {
            DataFetchStatus.Failed(it.fromStateData.input, it.transitionParameter)
        }
    }

    val reload by transitionsTo<Nothing, _>(inProgress) {
        from(received) {
            DataFetchStatus.InProgress(it.fromStateData.input, TODO())
        }
    }

    val retry by transitionsTo<Nothing, _>(inProgress) {
        from(cancelled, failed) {
            DataFetchStatus.InProgress(it.fromStateData.input, TODO())
        }
    }

    val changeInput by transitionsTo(inProgress) {
        from(undefined, inProgress, received, failed, cancelled) {
            DataFetchStatus.InProgress(it.transitionParameter, TODO())
        }
    }
}

interface StateMachineDispatcher<SMD : StateMachineDefinition> {

    fun <TransitionParameter, ToStateDataType> dispatch(block: SMD.() -> ExecutableTransition<TransitionParameter, ToStateDataType>)
}

interface InStateContext<SMD : StateMachineDefinition, StateDataType> : StateMachineDispatcher<SMD> {

    val currentState: StateDefinition<StateDataType>

    val currentStateData: StateDataType
}

interface DispatchDefinitionContext<SMD : StateMachineDefinition>

operator fun <TransitionParameter, ToStateDataType> TransitionEventDefinition<TransitionParameter, ToStateDataType>.invoke(
    parameter: TransitionParameter
): ExecutableTransition<TransitionParameter, ToStateDataType> =
    TODO()

interface ExecutionDefinitionContext<SMD : StateMachineDefinition> {

    val smd: SMD

    fun <StateDataType> inState(
        state: StateDefinition<StateDataType>,
        block: suspend InStateContext<SMD, StateDataType>.() -> Unit
    )

    fun <ToStateDateType> onTransition(
        transitionEventDefinition: TransitionEventDefinition<*, ToStateDateType>,
        block: () -> Unit
    )
}

interface ExecutableTransition<TransitionParameter, ToStateDataType> {

}

suspend fun <TransitionParameter, ToStateDataType, SMD : StateMachineDefinition> SMD.execute(
    executionDefinition: ExecutionDefinitionContext<SMD>.() -> ExecutableTransition<TransitionParameter, ToStateDataType>
): StateMachineDispatcher<SMD> {
    TODO()
}

suspend fun usecase() {
    data class FilteringData(val filterFragment: String)

    val dispatcher =
        DataFetchStateMachineDefinition<FilteringData, List<String>, Exception>().execute {
            with(smd) {
                inState(inProgress) {
                    println(currentStateData)
                    try {
                        delay(1000) // Simulate long network call
                        val result = listOf("a", "b", "c")
                        dispatch {
                            onReceived(result)
                        }
                    } catch (e: Exception) {
                        dispatch { onFailed(e) }
                    }
                }

                onTransition(cancel) {
                    // TODO log
                }

                changeInput(FilteringData(""))
            }
        }

//    println(dispatcher.currentState)
//    dispatcher.dispatch { changeInput(FilteringData("a")) }
//    dispatcher.stateFlow.collect {
//        println(it)
//    }
}

fun main() {
    DataFetchStateMachineDefinition<Nothing, Nothing, Nothing>() // TODO
}

//data class TransitionActionContext<SMD : StateMachineDefinition<SMD>, FromData, Parameter>(
//    val from: State<SMD, FromData>,
//    val parameter: Parameter
//)
//
//interface StateMachineDefinitionBinder<SMD : StateMachineDefinition<SMD>> {
//
//    val smd: SMD // TODO replace with context(SMD) in transitionAction
//
//    operator fun <Parameter> TransitionExecutor<SMD, Parameter>.invoke(transitionAction: TransitionActionContext<SMD, FromData, Parameter>.() -> Unit)
//}
//
//fun <SMD : StateMachineDefinition<SMD>> SMD.bind(binder: StateMachineDefinitionBinder<SMD>.() -> Unit) {
//
//}
//
//fun usecase() {
//    DataFetchStateMachineDefinition<Int, String, Exception>().bind {
//        with(smd) {
//            onInputChange {
//            }
//            onCancel {
//            }
//            onReceive {
//
//            }
//            onFailure {
//
//            }
//            onReload {
//
//            }
//            onRetry {
//
//            }
//        }
//    }
//}
