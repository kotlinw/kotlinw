package kotlinw.util.stdlib

import kotlin.test.Test
import kotlin.test.assertEquals

class MathUtilsTest {

    @Test
    fun testRound_0() {
        assertEquals(35.0, 35.4.round(0))
        assertEquals(36.0, 35.5.round(0))
        assertEquals(36.0, 35.87334.round(0))
    }

    @Test
    fun testRound_3() {
        assertEquals(35.4, 35.4.round(3))
        assertEquals(35.5, 35.5.round(3))
        assertEquals(35.873, 35.87334.round(3))
    }
}
