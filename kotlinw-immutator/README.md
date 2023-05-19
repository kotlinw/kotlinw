# Introduction

# Installation

# Usage

## Define the business model using interfaces

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

val mutated = original.mutate {
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
