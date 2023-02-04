package kotlinw.statemachine2

import kotlinw.util.stdlib.ClipboardUtils.copyToClipboard
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.memberProperties

fun <StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> SMD.exportDotToString(): String {

    fun TransitionDefinition<*, *, *, *, *>.isPublic() =
        this@exportDotToString::class.memberProperties.first { it == definingProperty }.visibility == PUBLIC

    fun TransitionDefinition<*, *, *, *, *>.edgeColor(): String = if (isPublic()) "black" else "gray"

    fun <ParameterType> TransitionDefinition<StateDataBaseType, SMD, ParameterType, *, *>.exportDotToString(): String =
        """${from.name} -> ${to.name} [label = "$eventName" color="${edgeColor()}" fontcolor="${edgeColor()}"];"""

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

fun <StateDataBaseType, SMD : StateMachineDefinition<StateDataBaseType, SMD>> SMD.exportDotToClipboard() {
    exportDotToString().copyToClipboard()
}

// TODO a nem public event-eket más színű éllel lehetne jelezni
