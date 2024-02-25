package xyz.kotlinw.util.unit

import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.kotlinw.util.unit.BinaryUnit.Bit.bit

class BinaryUnitTest {

    @Test
    fun testBinaryUnits() {
        assertEquals(8192, 8192.bit.scaledValue)
        assertEquals(8192, 8192.bit.value)

        assertEquals(8192, (8 Kibi bit).scaledValue)
        assertEquals(8000, (8 kilo bit).scaledValue)
        assertEquals(8, (8 Kibi bit).value)
        val a = 8.kilo.bit
        assertEquals(a, 8 kilo bit)

        assertEquals(8192, 8.Kibi.bit.scaledValue)
        assertEquals(8000, 8.kilo.bit.scaledValue)
    }
}
