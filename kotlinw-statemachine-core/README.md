# About

## About this library

This library contains a [state machine](https://en.wikipedia.org/wiki/Finite-state_machine) definition and execution implementation for Kotlin. It provides various level of typing support depending on how it is used.

## About state machines

A state machine is a computation model that can be used to represent and simulate sequential execution flow. 

A state machine contains states and defines the valid transitions from each state to others.\
Additionally it has an initial state where the execution starts and optionally terminal states where execution ends.

A classic state machine example is the ["coin-operated turnstile"](https://en.wikipedia.org/wiki/Finite-state_machine#Example:_coin-operated_turnstile):

![Turnstile state machine](doc/TurnstileStateMachine.png)

> A turnstile, used to control access to subways and amusement park rides, is a gate with three rotating arms at waist height, one across the entryway. Initially the arms are locked, blocking the entry, preventing patrons from passing through. Depositing a coin or token in a slot on the turnstile unlocks the arms, allowing a single customer to push through. After the customer passes through, the arms are locked again until another coin is inserted.
> 
> Considered as a state machine, the turnstile has two possible states: Locked and Unlocked. There are two possible inputs that affect its state: putting a coin in the slot (coin) and pushing the arm (push).

## Why use state machines

By modeling execution flows and state changes in your code with state machines, your code will be more readable, more compact and easier to follow :)

# Usage and example

Let's model the execution flow of the above turnstile state machine using this library.

## Add Gradle dependency

The library artifacts are available in the [Maven Central](https://repo.maven.apache.org/maven2/) repository, so simply
add the [kotlinw-statemachine-core](https://search.maven.org/search?q=g:xyz.kotlinw%20a:kotlinw-statemachine-core) dependency to your `build.gradle.kts` file.

In a multiplatform project

```
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("xyz.kotlinw:kotlinw-statemachine-core:0.4.0")
            }
        }
    }
}
```

In a platform-specific project:

```
dependencies {
    implementation("xyz.kotlinw:kotlinw-statemachine-core:0.4.0")
}
```

## Declare state machine class

Because this state machine does not have any additional data besides its current state, the state machine definition is stateless, so it can be an `object` and extend `SimpleStateMachineDefinition`:

```
object TurnstileStateMachineDefinition: SimpleStateMachineDefinition<TurnstileStateMachineDefinition>() {
}
```

## Define states

Declare a property for each state by using `state()`:

```
    val locked by state()

    val unlocked by state()
```

## Define transitions

Declare a property named `start` for the initial transition:

```
    override val start by initialTransitionTo(locked)
```

Declare one property for each valid transition:

```
    val insertCoin by unlocked.transitionFrom(locked)

    val pushArm by locked.transitionFrom(unlocked)
```

The final state machine definition class is:

```
object TurnstileStateMachineDefinition: SimpleStateMachineDefinition<TurnstileStateMachineDefinition>() {

    val locked by state()

    val unlocked by state()

    override val start by initialTransitionTo(locked)

    val insertCoin by unlocked.transitionFrom(locked)

    val pushArm by locked.transitionFrom(unlocked)
}
```

## Generate DOT representation

State transitions form a graph that can be visualized by [Graphviz](https://graphviz.org/).

To export the [DOT](https://graphviz.org/doc/info/lang.html) representation of the state machine's state transition graph to the clipboard, run the following code:

```
fun main() {
    TurnstileStateMachineDefinition.exportDotToClipboard()
}
```

The generated image is:

![Turnstile state machine](doc/TurnstileStateMachine.png)

## Configure actions

## Execute

## Use in Compose
