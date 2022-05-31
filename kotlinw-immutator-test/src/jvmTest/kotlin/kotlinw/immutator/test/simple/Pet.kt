package kotlinw.immutator.test.simple

import kotlinw.immutator.api.Immutate

enum class PetKind {
    Dog, Cat, Rabbit
}

@Immutate
sealed interface Pet {
    val kind: PetKind

    val name: String
}
