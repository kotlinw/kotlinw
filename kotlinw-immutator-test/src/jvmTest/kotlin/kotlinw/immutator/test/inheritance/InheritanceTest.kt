package kotlinw.immutator.test.inheritance

import kotlinw.immutator.api.Immutate
import kotlinw.immutator.test.inheritance.PetKind.Cat
import kotlinw.immutator.test.inheritance.PetKind.Dog
import kotlinw.immutator.test.inheritance.PetKind.Rabbit
import kotlinw.immutator.util.toImmutable
import kotlinw.immutator.util.toMutable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import java.time.Month.JANUARY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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

class InheritanceTest {
    @Test
    fun testNoModification() {
        val immutable = PersonImmutable(
            PersonNameImmutable(
                "Dr.",
                "Béla",
                "Kormos"
            ),
            LocalDate(2000, JANUARY, 1),
            "123456AB",
            persistentListOf(
                PetImmutable("Kutyi", Dog),
                PetImmutable("Nyuszi", Rabbit),
            ),
            HungarianAddressImmutable(1111, "Budapest", "Nyúl u. 1.", "HU"),
            persistentListOf(
                InternationalAddressImmutable("Somewhere...", "US"),
                HungarianAddressImmutable(1111, "Budapest", "Nyúl u. 1.", "HU")
            ).toImmutableList()
        )

        assertSame(immutable, immutable.toMutable().toImmutable())
    }

    @Test
    fun testInstanceCreation() {
        val immutable = PersonNameImmutable(null, "Béla", "Kovács")

        val mutable: PersonNameMutable = immutable.toMutable()
        assertEquals(immutable.title, mutable.title)
        assertEquals(immutable.firstName, mutable.firstName)
        assertEquals(immutable.lastName, mutable.lastName)
        assertEquals<PersonName>(immutable, mutable) // TODO report Idea bug: type inference fails

        val immutable2 = mutable.toImmutable()
        assertEquals(immutable, immutable2)
    }

    @Test
    fun testValuePropertyModification() {
        val immutable = PersonNameImmutable(null, "Béla", "Kovács")

        val mutable = immutable.toMutable()
        mutable.title = "Dr."
        mutable.firstName = "János"
        mutable.lastName = "Kormos"

        val immutable2 = mutable.toImmutable()
        assertEquals("Dr.", immutable2.title)
        assertEquals("János", immutable2.firstName)
        assertEquals("Kormos", immutable2.lastName)
    }

    @Test
    fun testReferencedObjectModification() {
        val immutable = PersonImmutable(
            PersonNameImmutable(
                null,
                "Béla",
                "Kormos"
            ),
            LocalDate(2000, JANUARY, 1),
            "123456AB",
            listOf(
                PetImmutable("Kutyi", Dog),
                PetImmutable("Nyuszi", Rabbit),
            ).toImmutableList(),
            HungarianAddressImmutable(1111, "Budapest", "Nyúl u. 1.", "HU"),
            persistentListOf()
        )

        val mutable = immutable.toMutable()
        assertEquals<Person>(immutable, mutable) // TODO report Idea bug: type inference fails

        mutable.name.lastName = "Haragos"

        val immutable2 = mutable.toImmutable()
        assertNotSame(immutable, immutable2)
        assertSame(immutable.birthdate, immutable2.birthdate)
        assertSame(immutable.identityCardNumber, immutable2.identityCardNumber)
        assertNotSame(immutable.name, immutable2.name)
        assertEquals(null, immutable2.name.title)
        assertEquals("Béla", immutable2.name.firstName)
        assertEquals("Haragos", immutable2.name.lastName)
    }

    @Test
    fun testReferencePropertyModification() {
        val immutable = PersonImmutable(
            PersonNameImmutable(
                "Dr.",
                "Béla",
                "Kormos"
            ),
            LocalDate(2000, JANUARY, 1),
            "123456AB",
            listOf(
                PetImmutable("Kutyi", Dog),
                PetImmutable("Nyuszi", Rabbit),
            ).toImmutableList(),
            HungarianAddressImmutable(1111, "Budapest", "Nyúl u. 1.", "HU"),
            persistentListOf()
        )

        val mutable = immutable.toMutable()
        assertEquals<Person>(immutable, mutable) // TODO report Idea bug: type inference fails

        mutable.name = PersonNameImmutable("Prof.", "Géza", "Heves").toMutable()

        val immutable2 = mutable.toImmutable()
        assertNotSame(immutable, immutable2)
        assertSame(immutable.birthdate, immutable2.birthdate)
        assertSame(immutable.identityCardNumber, immutable2.identityCardNumber)
        assertNotSame(immutable.name, immutable2.name)
        assertEquals("Prof.", immutable2.name.title)
        assertEquals("Géza", immutable2.name.firstName)
        assertEquals("Heves", immutable2.name.lastName)
    }

    @Test
    fun testListPropertyModification() {
        val immutable = PersonImmutable(
            PersonNameImmutable(
                "Dr.",
                "Béla",
                "Kormos"
            ),
            LocalDate(2000, JANUARY, 1),
            "123456AB",
            listOf(
                PetImmutable("Kutyi", Dog),
                PetImmutable("Nyuszi", Rabbit),
            ).toImmutableList(),
            HungarianAddressImmutable(1111, "Budapest", "Nyúl u. 1.", "HU"),
            persistentListOf()
        )

        assertEquals(Rabbit, immutable.pets[1].kind)

        val mutable = immutable.toMutable()
        mutable.pets.mutate {
            it[1].kind = Cat
        }

        val newImmutable = mutable.toImmutable()

        assertEquals(Cat, newImmutable.pets[1].kind)
        assertSame(immutable.pets[0], newImmutable.pets[0])
    }

    @Test
    fun testDeepModification() {
        val immutable = PersonImmutable(
            PersonNameImmutable(
                "Dr.",
                "Béla",
                "Kormos"
            ),
            LocalDate(2000, JANUARY, 1),
            "123456AB",
            persistentListOf(
                PetImmutable("Kutyi", Dog),
                PetImmutable("Nyuszi", Rabbit),
            ),
            HungarianAddressImmutable(1111, "Budapest", "Nyúl u. 1.", "HU"),
            persistentListOf(
                InternationalAddressImmutable("Somewhere...", "US"),
                HungarianAddressImmutable(1111, "Budapest", "Nyúl u. 1.", "HU")
            ).toImmutableList()
        )

        val mutable = immutable.toMutable()

        assertEquals<Person>(immutable, mutable) // TODO report Idea bug: type inference fails
        assertEquals<Person>(immutable, mutable.toImmutable())

        mutable.otherAddresses.mutate { (it[1] as HungarianAddressMutable).zipCode = 1112 }

        val newImmutable = mutable.toImmutable()

        assertSame(immutable.name, newImmutable.name)
        assertSame(immutable.mainAddress, newImmutable.mainAddress)
        assertSame(immutable.pets, newImmutable.pets)
        assertSame(immutable.otherAddresses[0], newImmutable.otherAddresses[0])
        assertNotSame(immutable.otherAddresses[1], newImmutable.otherAddresses[1])
        assertSame((immutable.otherAddresses[1] as HungarianAddress).city, (newImmutable.otherAddresses[1] as HungarianAddress).city)
    }
}
