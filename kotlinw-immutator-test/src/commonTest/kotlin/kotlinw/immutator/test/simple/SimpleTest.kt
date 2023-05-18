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
