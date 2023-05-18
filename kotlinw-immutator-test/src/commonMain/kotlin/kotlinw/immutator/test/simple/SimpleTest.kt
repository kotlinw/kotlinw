package kotlinw.immutator.test.simple

import kotlinw.immutator.annotation.Immutate
import kotlinx.datetime.LocalDate

// Inferred immutable class
data class Address(
    val city: String,
    val street: String
)

@Immutate
sealed interface Person {
    val name: PersonName

    val birthDate: LocalDate

    val address: Address

    val pets: List<Pet>
}

@Immutate
sealed interface PersonName {
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
    val kind: PetKind

    val name: String
}
