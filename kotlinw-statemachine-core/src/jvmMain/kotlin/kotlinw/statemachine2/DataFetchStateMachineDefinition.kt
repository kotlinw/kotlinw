package kotlinw.statemachine2

/**
 * Represents the status of fetching data of type [DataType] from a source specified by [InputType].
 *
 * @param InputType defines the source of the data (eg. an URL, a filter definition, etc.)
 * @param DataType type of the data to fetch
 * @param ErrorType describes an error occurred during fetching the data
 */
sealed interface DataFetchStatus<InputType> {

    val input: InputType

    /**
     * Data fetching is in progress.
     */
    data class InProgress<InputType>(
        override val input: InputType,
    ) : DataFetchStatus<InputType>

    /**
     * Data has been received.
     */
    data class Received<InputType, DataType>(
        override val input: InputType,
        val data: DataType
    ) : DataFetchStatus<InputType>

    /**
     * Data fetch has been cancelled.
     */
    data class Cancelled<InputType>(
        override val input: InputType
    ) : DataFetchStatus<InputType>

    /**
     * Data fetch has failed.
     */
    data class Failed<InputType, ErrorType>(
        override val input: InputType,
        val error: ErrorType
    ) : DataFetchStatus<InputType>
}

class DataFetchStateMachineDefinition<InputType, DataType, ErrorType> :
    StateMachineDefinition<DataFetchStatus<InputType>, DataFetchStateMachineDefinition<InputType, DataType, ErrorType>>() {

    val inProgress by state<DataFetchStatus.InProgress<InputType>>()

    val received by state<DataFetchStatus.Received<InputType, DataType>>()

    val cancelled by state<DataFetchStatus.Cancelled<InputType>>()

    val failed by state<DataFetchStatus.Failed<InputType, ErrorType>>()

    val start by initialTransitionTo(inProgress) {
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
