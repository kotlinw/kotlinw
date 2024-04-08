package kotlinw.util.stdlib

import arrow.core.identity
import arrow.core.raise.Raise
import arrow.core.raise.fold
import arrow.core.raise.recover
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class RaiseSemanticsTest {

    data object DivisionByZero

    context(Raise<DivisionByZero>)
    private fun div(a: Int, b: Int): Int =
        if (b != 0) {
            a / b
        } else {
            raise(DivisionByZero)
        }

    @Test
    fun testFoldExceptionHandling() {
        try {
            fold<DivisionByZero, Int, Int>({
                div(1, 0)
            }, {
                throw IllegalStateException("catch", it)
            }, {
                throw IllegalStateException("recover: $it")
            },
                ::identity
            )
            fail()
        } catch (e: Exception) {
            assertEquals("recover: $DivisionByZero", e.message)
        }
    }

@Test
fun testRecoverExceptionHandling() {
    try {
        recover<DivisionByZero, Int>({
            div(1, 0)
        }, {
            throw IllegalStateException("recover: $it")
        }, {
            throw IllegalStateException("catch", it)
        })
        fail()
    } catch (e: Exception) {
        assertEquals("recover: $DivisionByZero", e.message)
    }
}
}
