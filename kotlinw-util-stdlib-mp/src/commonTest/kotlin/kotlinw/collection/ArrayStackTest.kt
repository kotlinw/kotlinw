package kotlinw.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArrayStackTest {

    @Test
    fun testEmpty() {
        ArrayStack<Int>().apply {
            assertTrue(isEmpty())
            assertEquals(0, size)
            assertFalse(iterator().hasNext())
            assertFailsWith<NoSuchElementException> { iterator().next() }
        }
    }

    @Test
    fun testPushPop() {
        ArrayStack<Int>().apply {
            assertEquals(0, size)
            assertEquals(null, popOrNull())

            push(1)

            assertEquals(1, size)
            assertEquals(1, popOrNull())

            assertEquals(0, size)
            assertEquals(null, popOrNull())
        }

        ArrayStack<Int>().apply {
            addAll(listOf(1, 2, 3, 4, 5))
            assertEquals(5, size)
            assertEquals(listOf(5, 4, 3, 2, 1), toList())
            assertEquals(5, popOrNull())
            assertEquals(listOf(4, 3, 2, 1), toList())
            assertEquals(4, popOrNull())
            assertEquals(listOf(3, 2, 1), toList())
            push(4)
            assertEquals(listOf(4, 3, 2, 1), toList())
            clear()
            assertEquals(emptyList(), toList())
        }
    }
}
