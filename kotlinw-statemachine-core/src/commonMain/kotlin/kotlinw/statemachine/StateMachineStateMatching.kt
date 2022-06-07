package kotlinw.statemachine

interface MatcherScope<SMDefinition : StateMachineDefinition<SMDefinition>, T> {
    val stateMachineDefinition: SMDefinition

    fun <MatchedStateDataType> onState(
        stateDefinition: SMDefinition.() -> StateDefinition<SMDefinition, MatchedStateDataType>,
        block: (MatchedStateDataType) -> T
    )

    fun onElse(
        block: () -> T
    )
}

fun <SMDefinition : StateMachineDefinition<SMDefinition>, T> State<SMDefinition, *>.match(matcher: MatcherScope<SMDefinition, T>.() -> Unit): T {
    class Holder {
        private var _result: T? = null

        private var _isResultSet = false

        val isResultSet get() = _isResultSet

        var result: T?
            get() = _result
            set(value) {
                check(!isResultSet) { "Multiple matches!" }
                _result = value
                _isResultSet = true
            }
    }

    val resultHolder = Holder()

    val matchedStateDefinition = this@match.definition
    val stateMachineDefinition = matchedStateDefinition.stateMachineDefinition

    (object : MatcherScope<SMDefinition, T> {
        override fun <MatchedStateDataType> onState(
            stateDefinition: SMDefinition.() -> StateDefinition<SMDefinition, MatchedStateDataType>,
            block: (MatchedStateDataType) -> T
        ) {
            if (matchedStateDefinition == stateMachineDefinition.stateDefinition()) {
                resultHolder.result = block(this@match.data as MatchedStateDataType)
            }
        }

        override fun onElse(block: () -> T) {
            if (!resultHolder.isResultSet) {
                resultHolder.result = block()
            }
        }

        override val stateMachineDefinition: SMDefinition get() = stateMachineDefinition
    }).matcher()

    return if (resultHolder.isResultSet) resultHolder.result as T else throw IllegalStateException("No match.")
}
