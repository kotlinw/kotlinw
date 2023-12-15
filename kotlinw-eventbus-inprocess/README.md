# kotlinw-eventbus-inprocess

## Overview

`kotlinw-eventbus-inprocess` is a Kotlin library that provides an in-process event bus mechanism based on Kotlin coroutines.

It helps to decouple your application components using an event-driven architecture.

## Setup

Adding the library to your Gradle project:

```kotlin
dependencies {
    implementation("xyz.kotlinw:kotlinw-eventbus-inprocess:$LATEST_VERSION")
}
```

## Getting Started

The key interface in this library is `InProcessEventBus`, which can be used to publish events and add event handlers.
Creating an event bus instance:

```kotlin
val eventBus = InProcessEventBus()
```

## Publishing events

Events are simply objects, no further restriction applies to them. \
An example event class:

```kotlin
data class MessageEvent(val message: String)

data class SomeOtherEvent(val number: Int)
```

To publish an instance of `MessageEvent`:

```kotlin
eventBus.publish(MessageEvent("some message"))
```

## Handling events

Event handlers can be added to the event bus using the functions:

- `on()`: suspend and handle events (never completes normally)
- `asyncOn()`: return immediately, and handle events in a separately launched coroutine
- `once()`: suspend and handle _the first_ event published to the event bus
- `asyncOnce()`: return immediately, and handle _the first_ event published to the event bus in a separately launched coroutine 

In most cases the event handlers are interested only in specific types of events, so all of these functions expect the type of the events to listen for.\
For example to listen for only `MessageEvent`s:

```kotlin
eventBus.on<MessageEvent> { event: MessageEvent ->
    println("Message received: ${event.message}")
}
```

To listen for only the first `MessageEvent` asynchronously:

```kotlin
coroutineScope {
    val deferredMessage: Deferred<String> = eventBus.asyncOnce<MessageEvent, _>(this) { it.message }
    // Do some work...
    println(deferredMessage.await())
}
```

When adding an event handler, the `filter` predicate can be used to further filter to events the listener is interested in:

```kotlin
eventBus.on<MessageEvent>(
    filter = { it.message.startsWith("Interesting: ") }
) {
    println("Interesting message received: ${it.message}")
}

eventBus.publish(MessageEvent("Interesting: Kotlin 2.0 will be out soon!"))
eventBus.publish(MessageEvent("Boring: matrix multiplication is not commutative."))
```

## Dealing with slow event handlers
