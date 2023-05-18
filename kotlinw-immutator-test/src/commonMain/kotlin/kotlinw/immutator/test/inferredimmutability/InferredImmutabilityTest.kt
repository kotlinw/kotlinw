package kotlinw.immutator.test.inferredimmutability

import kotlinw.immutator.annotation.Immutate

data class Data(val s: String)

@Immutate
sealed interface TestClass {

    companion object

    val d: Data
}
