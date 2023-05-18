package kotlinw.immutator.test.inheritance

import kotlinw.immutator.annotation.Immutate
import kotlinx.datetime.LocalDate

@Immutate
sealed interface Person {
    companion object

    val name: PersonName

    val birthdate: LocalDate

    val identityCardNumber: String

    val pets: List<Pet>

    val mainAddress: Address

    val otherAddresses: List<Address>
}

@Immutate
sealed interface Address {
    val countryCode: String
}

@Immutate
sealed interface HungarianAddress: Address {
    val zipCode: Int
    val city: String
    val address: String
}

@Immutate
sealed interface InternationalAddress: Address {
    val genericAddress: String
}

@Immutate
sealed interface Pet {
    companion object

    val name: String

    val kind: PetKind
}

enum class PetKind {
    Cat, Dog, Rabbit
}

@Immutate
sealed interface PersonName {
    companion object

    val title: String?

    val firstName: String

    val lastName: String
}
