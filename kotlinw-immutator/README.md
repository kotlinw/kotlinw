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

For user code, the only important is the extension function used to create immutable instances,
for example in case of `Pet` the following is generated:

```kotlin
fun Pet.Companion.immutable(kind: PetKind, name: String): PetImmutable = ...
```

Besides, the following class declarations are generated as well for each *definition interface*:

- a `data class` with the immutable implementation
- an `interface` with the mutable variant of the *definition interface*
- (an internal `data class` with the mutable implementation)

## Create immutable instances

```kotlin
val original = Person.immutable(
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
assertSame(original, original.toMutable().toImmutable())

val mutated = original.mutate { /* it: PersonMutable -> */
    it.pets[1].name = "Doggo"
}

assertEquals("Doggy", original.pets[1].name)
assertEquals("Doggo", mutated.pets[1].name)

assertNotSame(original, mutated)
assertSame(original.name, mutated.name)
assertSame(original.address, mutated.address)
assertNotSame(original.pets, mutated.pets)
assertSame(original.pets[0], mutated.pets[0])
assertNotSame(original.pets[1], mutated.pets[1])
```
