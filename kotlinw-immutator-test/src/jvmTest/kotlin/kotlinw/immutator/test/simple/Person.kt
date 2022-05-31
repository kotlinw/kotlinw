package kotlinw.immutator.test.simple

import kotlinw.immutator.api.Immutate
import kotlinx.datetime.LocalDate

@Immutate
sealed interface Person {
    val name: PersonName

    val birthDate: LocalDate

    val address: Address

    val pets: List<Pet>
}
