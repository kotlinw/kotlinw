package kotlinw.util.stdlib.collection

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableListUtilTest {

    @Test
    fun testDropInPlace() {
        assertEquals(listOf(1, 2, 3, 4), mutableListOf(1, 2, 3, 4).apply { dropInPlace(0) })
        assertEquals(listOf(2, 3, 4), mutableListOf(1, 2, 3, 4).apply { dropInPlace(1) })
        assertEquals(listOf(3, 4), mutableListOf(1, 2, 3, 4).apply { dropInPlace(2) })
        assertEquals(listOf(4), mutableListOf(1, 2, 3, 4).apply { dropInPlace(3) })
        assertEquals(emptyList(), mutableListOf(1, 2, 3, 4).apply { dropInPlace(4) })
        assertEquals(emptyList(), mutableListOf(1, 2, 3, 4).apply { dropInPlace(5) })
    }
}
