package kotlinw.immutator.test.inferredimmutability

import kotlinw.immutator.annotation.Immutate

data class Data(val s: String)

@Immutate
sealed interface TestClass {
    val d: Data
}
