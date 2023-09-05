package kotlinw.immutator.test.simple

import kotlinw.immutator.annotation.Immutate
import kotlinw.immutator.test.simple.PetKind.Dog
import kotlinw.immutator.test.simple.PetKind.Rabbit
import kotlinw.immutator.util.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month.FEBRUARY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class SimpleTest {

    @Test
    fun testIdentitySemantics() {
        val original = Person.immutable(
            PersonName.immutable(null, "John", "Doe"),
            LocalDate(1985, FEBRUARY, 22),
            Address("City", "Street"),
            persistentListOf(
                Pet.immutable(kind = Rabbit, name = "Bunny"),
                Pet.immutable(kind = Dog, name = "Doggy")
            )
        )

        // Mutation is effecient, if nothing is modified then no new object is created
        assertSame(original, original.toMutable().toImmutable())

        // Mutation works on truly mutable instances, not by explicitly copying the existing instances
        // (copying happens implicitly in the background if needed)
        val mutated = original.mutate {
            it.name.title = "Prof."
            it.pets[1].name = "Doggo"
        }

        assertNotSame(original, mutated)

        // Non-mutated properties retain referential equality (no unnecessary copies are created)
        assertSame(original.address, mutated.address)
        assertSame(original.pets[0], mutated.pets[0])

        assertNotSame(original.name, mutated.name)
        assertNull(original.name.title)
        assertEquals("Prof.", mutated.name.title)

        assertNotSame(original.pets, mutated.pets)
        assertNotSame(original.pets[1], mutated.pets[1])
        assertEquals("Doggy", original.pets[1].name)
        assertEquals("Doggo", mutated.pets[1].name)
    }
}
