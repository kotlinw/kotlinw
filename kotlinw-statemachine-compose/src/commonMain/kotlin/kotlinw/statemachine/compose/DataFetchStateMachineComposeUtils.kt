package kotlinw.statemachine.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinw.statemachine.StateMachineDefinition
import kotlinw.statemachine.match
import kotlinw.statemachine.util.DataFetchStateMachineDefinition
import kotlinw.statemachine.util.DataFetchStateMachineDefinition.DataFetchInProgressData
import kotlinw.statemachine.util.DataFetchStatus
import kotlinw.statemachine.util.DataFetchStatus.DataAvailable
import kotlinw.statemachine.util.DataFetchStatus.DataFetchFailed
import kotlinw.statemachine.util.DataFetchStatus.DataFetchInProgress

@Composable
fun <InputType, DataType> produceDataFetchState(
    inputParameter: InputType,
    dataProducer: @DisallowComposableCalls suspend (InputType) -> DataType
): State<DataFetchStatus<InputType, out DataType>> {
    val coroutineScope = rememberCoroutineScope()
    val stateMachineDefinition by derivedStateOf {
        DataFetchStateMachineDefinition<InputType, DataType>(coroutineScope) { dataProducer(it) }
    }

    val composeState by rememberStateMachineState(
        stateMachineDefinition = stateMachineDefinition,
        key1 = inputParameter,
        initializer = { dispatch(definition.onInputChanged, inputParameter) },
        finalizer = {
            if (it.data is DataFetchInProgressData<*>) {
                dispatch(definition.onCancelLoading)
            }
        }
    )

    fun reload() {
        composeState.dispatcher.dispatch(stateMachineDefinition.onReload, inputParameter)
    }

    return derivedStateOf {
        composeState.currentState.match {
            onState({ loading }) {
                DataFetchInProgress(
                    parameter = it.inputParameter,
                    cancel = { composeState.dispatcher.dispatch(stateMachineDefinition.onCancelLoading) }
                )
            }
            onState({ displayingData }) {
                DataAvailable(
                    parameter = it.inputParameter,
                    resultData = it.data,
                    reload = ::reload
                )
            }
            onState({ displayingError }) {
                DataFetchFailed(
                    parameter = it.inputParameter,
                    errorMessage = it.errorMessage,
                    reload = ::reload
                )
            }
            onElse {
                throw IllegalStateException()
            }
        }
    }
}
