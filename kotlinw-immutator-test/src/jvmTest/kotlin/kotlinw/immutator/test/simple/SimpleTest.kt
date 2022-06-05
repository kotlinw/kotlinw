package kotlinw.immutator.test.simple

import kotlinw.immutator.annotation.Immutate
import kotlinw.immutator.test.simple.PetKind.Dog
import kotlinw.immutator.test.simple.PetKind.Rabbit
import kotlinw.immutator.util.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import java.time.Month.FEBRUARY
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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

class SimpleTest {
    @Test
    fun testIdentitySemantics() {
        val original = PersonImmutable(
            PersonNameImmutable(null, "John", "Doe"),
            LocalDate(1985, FEBRUARY, 22),
            Address("City", "Street"),
            persistentListOf(
                PetImmutable(kind = Rabbit, name = "Bunny"),
                PetImmutable(kind = Dog, name = "Doggy")
            )
        )

        assertSame(original, original._immutator_convertToMutable()._immutator_convertToImmutable())

        val mutated = original.mutate {
            it.pets[1].name = "Doggo"
        }

        assertNotSame(original, mutated)
        assertSame(original.name, mutated.name)
        assertSame(original.address, mutated.address)
        assertNotSame(original.pets, mutated.pets)
        assertSame(original.pets[0], mutated.pets[0])
        assertNotSame(original.pets[1], mutated.pets[1])
    }
}
