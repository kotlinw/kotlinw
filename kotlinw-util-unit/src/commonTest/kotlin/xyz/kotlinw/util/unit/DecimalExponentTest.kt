package xyz.kotlinw.util.unit

import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.kotlinw.util.unit.DecimalExponent.Companion.exponentFor
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.M1
import xyz.kotlinw.util.unit.DecimalExponent.ConstantDecimalExponent.P1
import xyz.kotlinw.util.unit.DecimalExponent.Div
import xyz.kotlinw.util.unit.DecimalExponent.Excluded
import xyz.kotlinw.util.unit.DecimalExponent.Mul

class DecimalExponentTest {

    @Test
    fun testExponentFor() {
        assertEquals(null, exponentFor<Excluded>())
        assertEquals(-1, exponentFor<M1>())
        assertEquals(1, exponentFor<P1>())

        assertEquals(null, exponentFor<Mul<Excluded, Excluded>>())
        assertEquals(-1, exponentFor<Mul<M1, Excluded>>())
        assertEquals(-1, exponentFor<Mul<Excluded, M1>>())
        assertEquals(-2, exponentFor<Mul<M1, M1>>())
        assertEquals(1, exponentFor<Mul<Excluded, P1>>())
        assertEquals(2, exponentFor<Mul<P1, P1>>())
        assertEquals(0, exponentFor<Mul<P1, M1>>())

        assertEquals(null, exponentFor<Div<Excluded, Excluded>>())
        assertEquals(-1, exponentFor<Div<M1, Excluded>>())
        assertEquals(1, exponentFor<Div<Excluded, M1>>())
        assertEquals(0, exponentFor<Div<M1, M1>>())
        assertEquals(-1, exponentFor<Div<Excluded, P1>>())
        assertEquals(0, exponentFor<Div<P1, P1>>())
        assertEquals(2, exponentFor<Div<P1, M1>>())
    }
}
