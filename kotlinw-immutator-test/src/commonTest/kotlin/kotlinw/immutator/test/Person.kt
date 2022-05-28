package kotlinw.immutator.test

import kotlinw.immutator.api.Immutate

@Immutate
interface Person {
    val name: String
}
