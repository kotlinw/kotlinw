package statemachine2

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

sealed interface DataFetchStatus<InputType, DataType, ErrorType> {

    val input: InputType

    data class InProgress<InputType>(
        override val input: InputType,
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
    StateMachineDefinition<DataFetchStatus<InputType, DataType, ErrorType>, DataFetchStateMachineDefinition<InputType, DataType, ErrorType>>() {

    val inProgress by state<DataFetchStatus.InProgress<InputType>>()

    val received by state<DataFetchStatus.Received<InputType, DataType>>()

    val cancelled by state<DataFetchStatus.Cancelled<InputType>>()

    val failed by state<DataFetchStatus.Failed<InputType, ErrorType>>()

    // TODO val cancel = inProgress transitionTo cancelled {}

    val cancel by transitionsTo<Unit, _>(cancelled) {
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

    val reload by transitionsTo<Unit, _>(inProgress) {
        from(received) {
            DataFetchStatus.InProgress(it.fromStateData.input)
        }
    }

    val retry by transitionsTo<Unit, _>(inProgress) {
        from(cancelled, failed) {
            DataFetchStatus.InProgress(it.fromStateData.input)
        }
    }

    val changeInput by transitionsTo(inProgress) {
        from(undefined, inProgress, received, failed, cancelled) {
            DataFetchStatus.InProgress(it.transitionParameter)
        }
    }
}

fun main() {
    DataFetchStateMachineDefinition<Nothing, Nothing, Nothing>() // TODO
}
