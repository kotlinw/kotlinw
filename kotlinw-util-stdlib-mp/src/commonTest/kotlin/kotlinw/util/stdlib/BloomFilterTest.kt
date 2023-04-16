package kotlinw.util.stdlib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BloomFilterTest {

    @Test
    fun verifyExpectedHashCodes() {
        assertEquals(0b000, 0.hashCode())
        assertEquals(0b001, 1.hashCode())
        assertEquals(0b010, 2.hashCode())
        assertEquals(0b011, 3.hashCode())
    }

    @Test
    fun testTooSmallSize() {
        with(newMutableBloomFilter<Int>(1)) {

            assertFalse(mightContain(0))
            assertFalse(mightContain(1))
            assertFalse(mightContain(2))
            assertFalse(mightContain(3))

            add(0)

            assertTrue(mightContain(0))
            assertTrue(mightContain(1))
            assertTrue(mightContain(2))
            assertTrue(mightContain(3))
        }
    }

    @Test
    fun testAppropriateSmallSize() {
        with(newMutableBloomFilter<Int>(4)) {

            assertFalse(mightContain(0))
            assertFalse(mightContain(1))
            assertFalse(mightContain(2))
            assertFalse(mightContain(3))

            add(0)

            assertTrue(mightContain(0))
            assertFalse(mightContain(1))
            assertFalse(mightContain(2))
            assertFalse(mightContain(3))

            add(1)

            assertTrue(mightContain(0))
            assertTrue(mightContain(1))
            assertFalse(mightContain(2))
            assertFalse(mightContain(3))

            add(2)

            assertTrue(mightContain(0))
            assertTrue(mightContain(1))
            assertTrue(mightContain(2))
            assertFalse(mightContain(3))

            add(3)

            assertTrue(mightContain(0))
            assertTrue(mightContain(1))
            assertTrue(mightContain(2))
            assertTrue(mightContain(3))

            // Use 999 instead of Int.MAX_VALUE for performance
            (4..999).forEach {
                assertTrue(mightContain(it))
            }
        }
    }
}
