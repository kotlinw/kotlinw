package kotlinw.statemachine.datafetch

import kotlinw.statemachine.StateMachineDefinition
import kotlinw.statemachine.TransitionContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DataFetchStateMachineDefinition<InputType, DataType>(
    private val coroutineScope: CoroutineScope,
    private val dataProducer: suspend (InputType) -> DataType
) :
    StateMachineDefinition<DataFetchStateMachineDefinition<InputType, DataType>>() {

    data class DataFetchInProgressData<InputType>(
        val inputParameter: InputType,
        val dataFetchJob: Job
    )

    val loading by state<DataFetchInProgressData<InputType>>()

    data class DataFetchSucceededData<InputType, DataType>(
        val inputParameter: InputType,
        val data: DataType
    )

    val displayingData by state<DataFetchSucceededData<InputType, DataType>>()

    data class DataFetchFailedData<InputType>(
        val inputParameter: InputType,
        val errorMessage: String
    )

    val displayingError by state<DataFetchFailedData<InputType>>()

    val onCancelLoading by transitionsTo<DataFetchFailedData<InputType>, Unit>(displayingError) {
        from(loading) {
            from.dataFetchJob.cancel()

            DataFetchFailedData(
                from.inputParameter,
                "Cancelled."
            ) // TODO i18n, hibakód?
        }
    }

    private val onDataReceived by transitionsTo<DataFetchSucceededData<InputType, DataType>, DataType>(displayingData) {
        from(loading) {
            DataFetchSucceededData(
                from.inputParameter,
                parameter
            )
        }
    }

    private val onLoadingFailed by transitionsTo<DataFetchFailedData<InputType>, String>(displayingError) {
        from(loading) {
            DataFetchFailedData(from.inputParameter, parameter)
        }
    }

    val onReload by transitionsTo<DataFetchInProgressData<InputType>, InputType>(loading) {
        from(displayingError) { doLoading(null, from.inputParameter) }
        from(displayingData) { doLoading(null, from.inputParameter) }
    }

    val onInputChanged by transitionsTo<DataFetchInProgressData<InputType>, InputType>(loading) {
        from(undefined) { doLoading(null, parameter) }
        from(loading) { doLoading(from.dataFetchJob, from.inputParameter) }
        from(displayingData) { doLoading(null, from.inputParameter) }
        from(displayingError) { doLoading(null, from.inputParameter) }
    }

    private fun TransitionContext<*, *, DataFetchStateMachineDefinition<InputType, DataType>>.doLoading(
        dataFetchJob: Job?,
        inputParameter: InputType
    ): DataFetchInProgressData<InputType> {
        dataFetchJob?.cancel()

        return DataFetchInProgressData(inputParameter, coroutineScope.launch {
            try {
                dispatch(definition.onDataReceived, dataProducer(inputParameter))
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                dispatch(
                    definition.onLoadingFailed,
                    e.message?.let { "Failed: ${e.message}" } ?: "Failed."
                ) // TODO i18n, hibakód?
            }
        })
    }
}

sealed class DataFetchStatus<InputType, DataType> {
    abstract val parameter: InputType

    data class DataFetchInProgress<InputType>(
        override val parameter: InputType,
        val cancel: () -> Unit
    ) : DataFetchStatus<InputType, Nothing>()

    data class DataAvailable<InputType, DataType>(
        override val parameter: InputType,
        val resultData: DataType,
        val reload: () -> Unit
    ) : DataFetchStatus<InputType, DataType>()

    data class DataFetchFailed<InputType>(
        override val parameter: InputType,
        val errorMessage: String,
        val reload: () -> Unit
    ) : DataFetchStatus<InputType, Nothing>()
}
