package kotlinw.statemachine2

object TurnstileStateMachineDefinition: SimpleStateMachineDefinition<TurnstileStateMachineDefinition>() {

    val locked by state()

    val unlocked by state()

    val start by initialTransitionTo(locked)

    val insertCoin by unlocked.from(locked)

    val pushArm by transitionTo(locked).from(unlocked) // transitionTo() is optional but slightly increases readability
}

fun main() {
    TurnstileStateMachineDefinition.exportDotToClipboard()
}
