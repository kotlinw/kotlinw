package kotlinw.statemachine2

sealed interface DataFetchStatus<InputType, DataType, ErrorType> {

    val input: InputType

    data class InProgress<InputType, DataType, ErrorType>(
        override val input: InputType,
    ) : DataFetchStatus<InputType, DataType, ErrorType>

    data class Received<InputType, DataType, ErrorType>(
        override val input: InputType,
        val data: DataType
    ) : DataFetchStatus<InputType, DataType, ErrorType>

    data class Cancelled<InputType, DataType, ErrorType>(
        override val input: InputType
    ) : DataFetchStatus<InputType, DataType, ErrorType>

    data class Failed<InputType, DataType, ErrorType>(
        override val input: InputType,
        val error: ErrorType
    ) : DataFetchStatus<InputType, DataType, ErrorType>
}

class DataFetchStateMachineDefinition<InputType, DataType, ErrorType> :
    StateMachineDefinition<DataFetchStatus<InputType, DataType, ErrorType>, DataFetchStateMachineDefinition<InputType, DataType, ErrorType>>() {

    val inProgress by state<DataFetchStatus.InProgress<InputType, DataType, ErrorType>>()

    val received by state<DataFetchStatus.Received<InputType, DataType, ErrorType>>()

    val cancelled by state<DataFetchStatus.Cancelled<InputType, DataType, ErrorType>>()

    val failed by state<DataFetchStatus.Failed<InputType, DataType, ErrorType>>()

    override val start by initialTransitionTo(inProgress) {
        DataFetchStatus.InProgress(it.transitionParameter)
    }

    val cancel by cancelled.from<Unit, _, _>(inProgress) {
        DataFetchStatus.Cancelled(it.fromStateData.input)
    }

    internal val onReceived by received.from(inProgress) {
        DataFetchStatus.Received(it.fromStateData.input, it.transitionParameter)
    }

    internal val onFailed by failed.from(inProgress) {
        DataFetchStatus.Failed(it.fromStateData.input, it.transitionParameter)
    }

    val reload by inProgress.from<Unit, _, _>(received) {
        DataFetchStatus.InProgress(it.fromStateData.input)
    }

    val retry by inProgress.from<Unit, _, _>(cancelled, failed) {
        DataFetchStatus.InProgress(it.fromStateData.input)
    }

    val changeInput by inProgress.from(inProgress, received, failed, cancelled) {
        DataFetchStatus.InProgress(it.transitionParameter)
    }
}

fun main() {
    DataFetchStateMachineDefinition<Nothing, Nothing, Nothing>().exportDotToClipboard()
}
