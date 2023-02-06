# About

# About this library

This library contains a type-safe [state machine](https://en.wikipedia.org/wiki/Finite-state_machine) implementation for Kotlin.

# About state machines

A state machine is a computation model that can be used to represent and simulate sequential execution flow. 

A state machine contains states and defines the valid transitions from each state to others.\
Additionally it has an initial state where the execution starts and optionally terminal states where execution ends.

An example state machine for fetching data:

![Data fetch state machine](doc/DataFetchStateMachine.png)

# Why

By modeling execution flows in your code with state machines your code will be more readable, more compact and easier to follow :)

# Usage

## Add Gradle dependency

The library artifacts are available in the [Maven Central](https://repo.maven.apache.org/maven2/) repository, so simply
add the [kotlinw-statemachine-core](https://search.maven.org/search?q=g:xyz.kotlinw%20a:kotlinw-statemachine-core) dependency to your `build.gradle.kts` file.

In a multiplatform project

```
kotlin {
    ...
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("xyz.kotlinw:kotlinw-statemachine-core:0.4.0")

```

In a platform-specific project:

```
dependencies {
    implementation("xyz.kotlinw:kotlinw-statemachine-core:0.4.0")
```

Note that if you have a custom [`repositories { ... }`](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:repositories(groovy.lang.Closure)) block in your build file then the [mavenCentral()](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.dsl.RepositoryHandler.html#org.gradle.api.artifacts.dsl.RepositoryHandler:mavenCentral()) repository should be added explicitly:

```
buildscript {
    repositories {
        mavenCentral()
        ...
    }
}
```

## Define state machine 

### Declare state data types 

### Define states

### Define transitions

## Generate image representation

## Configure actions

## Execute

## Use in Compose
