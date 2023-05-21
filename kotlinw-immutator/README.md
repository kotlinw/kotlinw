# Introduction

# Installation

# Usage

## Define the domain model using interfaces

Declare `interface`s of the domain model's immutable variant, annotated with `@Immutate` (we call these *definition
interface*s):

```kotlin
import kotlinw.immutator.annotation.Immutate
import kotlinx.datetime.LocalDate

@Immutate
sealed interface Person {

    companion object

    val name: PersonName

    val birthDate: LocalDate

    val address: Address

    val pets: List<Pet>
}

// Inferred immutable class
data class Address(
    val city: String,
    val street: String
)

@Immutate
sealed interface PersonName {

    companion object

    val title: String?

    val firstName: String

    val lastName: String

    val fullName get() = (if (title != null) "$title " else "") + firstName + " " + lastName
}

enum class PetKind {
    Dog, Cat, Rabbit
}

@Immutate
sealed interface Pet {

    companion object

    val kind: PetKind

    val name: String
}
```

## Build the project

By building the project, KSP generates various declarations from the annotated *definition interface* declarations.

The most important ones are (with simplified code examples):

- a `class` with the immutable implementation, e.g.
    ```kotlin
    class PetImmutable(
        override val kind: PetKind,
        override val name: String,
    ) : Pet
    ```
- an extension function to create immutable instances, e.g.
    ```kotlin
    fun Pet.Companion.immutable(kind: PetKind, name: String): PetImmutable = ...
    ```
- an `interface` with the mutable variant of the *definition interface*, e.g.
    ```kotlin
    interface PetMutable : Pet {
        override var kind: PetKind
        override var name: String
    }
    ```
- an internally used `class` with the mutable implementation, e.g. `class PetMutableImpl(...): PetMutable`

## Create immutable instances

```kotlin
val original: PersonImmutable = Person.immutable(
    PersonName.immutable(null, "John", "Doe"),
    LocalDate(1985, FEBRUARY, 22),
    Address("City", "Street"),
    persistentListOf(
        Pet.immutable(kind = Rabbit, name = "Bunny"),
        Pet.immutable(kind = Dog, name = "Doggy")
    )
)
```

## Mutate the instances

```kotlin
// Mutation is effecient, if nothing is modified then no new object is created
assertSame(original, original.toMutable().toImmutable())

// Mutation works on truly mutable instances, not by explicitly copying the existing instances
// (copying happens implicitly in the background if needed)
val mutated = original.mutate {
  it.name.title = "Prof."
  it.pets[1].name = "Doggo"
}

assertNotSame(original, mutated)

// Non-mutated properties retain referential equality
assertSame(original.address, mutated.address)
assertSame(original.pets[0], mutated.pets[0])

assertNotSame(original.name, mutated.name)
assertNull(original.name.title)
assertEquals("Prof.", mutated.name.title)

assertNotSame(original.pets, mutated.pets)
assertNotSame(original.pets[1], mutated.pets[1])
assertEquals("Doggy", original.pets[1].name)
assertEquals("Doggo", mutated.pets[1].name)
```
