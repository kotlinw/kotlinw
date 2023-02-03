package statemachine2

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

interface StateMachineDispatcher<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    val currentState: StateDefinition<StateDataBaseType>

    val currentStateData: StateDataBaseType

    val stateFlow: SharedFlow<StateDataBaseType>

    fun <TransitionParameter, ToStateDataType> dispatch(block: SMD.() -> ExecutableTransition<TransitionParameter, ToStateDataType>)
}

operator fun <TransitionParameter, ToStateDataType> TransitionEventDefinition<TransitionParameter, ToStateDataType>.invoke(
    parameter: TransitionParameter
): ExecutableTransition<TransitionParameter, ToStateDataType> =
    TODO()

interface ExecutionDefinitionContext<StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> {

    fun <StateDataType> inState(
        state: StateDefinition<StateDataType>,
        block: suspend context(SMD) StateMachineDispatcher<StateDataBaseType, SMD>.() -> Unit
    )

    fun <FromStateDataType, ToStateDataType> onTransition(
        transitionEventDefinition: TransitionEventDefinition<FromStateDataType, ToStateDataType>,
        block: (FromStateDataType, ToStateDataType) -> Unit
    )
}

interface ExecutableTransition<TransitionParameter, ToStateDataType> {

}

suspend fun <StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>, TransitionParameter, ToStateDataType> SMD.execute(
    executionDefinition: context(SMD) ExecutionDefinitionContext<StateDataBaseType, SMD>.() -> ExecutableTransition<TransitionParameter, ToStateDataType>
): StateMachineDispatcher<StateDataBaseType, SMD> {
    TODO()
}

suspend fun usecase() {
    data class FilteringData(val filterFragment: String)

    val dispatcher =
        DataFetchStateMachineDefinition<FilteringData, List<String>, Exception>().execute {
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

            onTransition(cancel) { from, to ->
                println()
                // TODO log
            }

            changeInput(FilteringData(""))
        }

    println(dispatcher.currentState)
    dispatcher.dispatch { changeInput(FilteringData("a")) }
//    dispatcher.stateFlow.collect {
//        println(it)
//    }
}
