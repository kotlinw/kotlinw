package kotlinw.immutator.test.inferredimmutability

import kotlinw.immutator.annotation.Immutate
import kotlinw.immutator.util.mutate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

data class Data(val s: String)

@Immutate
sealed interface TestClass {
    val d: Data
}

class InferredImmutability {
    @Test
    fun test() {
        val o = TestClassImmutable(Data("a"))
        assertSame(o, o.toMutable().toImmutable())

        assertEquals(
            TestClassImmutable(Data("b")),
            o.mutate { it.d = Data("b") }
        )
    }
}
