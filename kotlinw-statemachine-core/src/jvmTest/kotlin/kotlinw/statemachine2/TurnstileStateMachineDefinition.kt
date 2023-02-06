package kotlinw.statemachine2

object TurnstileStateMachineDefinition: SimpleStateMachineDefinition<TurnstileStateMachineDefinition>() {

    val locked by state()

    val unlocked by state()

    override val start by initialTransitionTo(locked)

    val insertCoin by unlocked.transitionFrom(locked)

    val pushArm by locked.transitionFrom(unlocked)
}

fun main() {
    TurnstileStateMachineDefinition.exportDotToClipboard()
}
