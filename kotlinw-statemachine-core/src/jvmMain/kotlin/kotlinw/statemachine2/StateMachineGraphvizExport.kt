package kotlinw.statemachine2

import kotlinw.util.stdlib.ClipboardUtils.copyToClipboard

fun <StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> SMD.exportDotToString(): String {

    fun TransitionDefinition<*, *, *, *>.edgeColor(): String = if (isPublic) "black" else "gray"

    fun <ParameterType> TransitionDefinition<StateDataBaseType, SMD, ParameterType, *>.exportDotToString(): String =
        when (this) {
            is InitialTransitionDefinition<StateDataBaseType, SMD, ParameterType, *> -> undefined
            is NormalTransitionDefinition<StateDataBaseType, SMD, ParameterType, *, *> -> from

        }.let {
            """${it.name} -> ${to.name} [label = "$eventName" color="${edgeColor()}" fontcolor="${edgeColor()}"];"""
        }

    fun TransitionEventDefinition<StateDataBaseType, SMD, *, *, *>.exportDot(): List<String> =
        transitions.map { it.exportDotToString() }

    return """
        digraph finite_state_machine {
            graph [pad = "0.5", nodesep = "1", ranksep = "2"]
            fontname = "Helvetica,Arial,sans-serif"
            node [fontname = "Helvetica,Arial,sans-serif"]
            edge [fontname = "Helvetica,Arial,sans-serif"]
            rankdir = "LR"
            ${events.flatMap { it.exportDot() }.joinToString("\n")}
        }
    """.trimIndent()
}

fun <StateDataBaseType, SMD : StateMachineDefinitionBase<StateDataBaseType, SMD>> SMD.exportDotToClipboard() {
    exportDotToString().copyToClipboard()
}
