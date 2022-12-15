package kotlinw.statemachine.tool

import kotlinw.statemachine.StateMachineDefinition
import kotlinw.statemachine.TransitionDefinition
import kotlinw.statemachine.TransitionEventDefinition
import kotlinw.util.stdlib.ClipboardUtils.copyToClipboard
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.memberProperties

fun <SMDefinition : StateMachineDefinition<SMDefinition>> StateMachineDefinition<SMDefinition>.exportDotToString(): String {
    fun TransitionDefinition<*, *, *, *>.isPublic() =
        this@exportDotToString::class.memberProperties.first { it.name == definingPropertyName }.visibility == PUBLIC

    fun TransitionDefinition<*, *, *, *>.edgeColor(): String = if (isPublic()) "black" else "gray"

    fun <SMDefinition : StateMachineDefinition<SMDefinition>, ParameterType> TransitionDefinition<SMDefinition, *, *, ParameterType>.exportDotToString(): String =
        """${from.name} -> ${to.name} [label = "$eventName" color="${edgeColor()}" fontcolor="${edgeColor()}"];"""

    fun <SMDefinition : StateMachineDefinition<SMDefinition>> TransitionEventDefinition<SMDefinition>.exportDot(): List<String> =
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

fun <SMDefinition : StateMachineDefinition<SMDefinition>> SMDefinition.exportDotToClipboard() {
    exportDotToString().copyToClipboard()
}

// TODO a nem public event-eket más színű éllel lehetne jelezni
