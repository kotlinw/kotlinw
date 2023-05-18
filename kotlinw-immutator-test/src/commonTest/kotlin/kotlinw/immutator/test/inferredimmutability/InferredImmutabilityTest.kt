package kotlinw.immutator.test.inferredimmutability

import kotlinw.immutator.util.mutate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class InferredImmutabilityTest {
    @Test
    fun test() {
        val o = TestClass.new(Data("a"))
        assertSame(o, o.toMutable().toImmutable())

        assertEquals(
            TestClass.new(Data("b")),
            o.mutate { it.d = Data("b") }
        )
    }
}
