package kotlinw.collection

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImmutableArrayListTest {
    @Test
    fun testEmpty() {
        val ref: ImmutableList<Int> = persistentListOf()
        val impl = ImmutableArrayList.empty<Int>()

        fun testBasics(list: ImmutableList<Int>) {
            assertEquals(0, list.size)
            assertFailsWith<NoSuchElementException> { list.first() }
        }

        testBasics(ref)
        testBasics(impl)

        fun testIterator(list: ImmutableList<Int>) {
            val it = list.iterator()
            assertEquals(false, it.hasNext())
            assertFailsWith<NoSuchElementException> { it.next() }
        }

        testIterator(ref)
        testIterator(impl)

        fun testListIterator(list: ImmutableList<Int>) {
            val it = list.listIterator()
            assertEquals(false, it.hasNext())
            assertEquals(false, it.hasPrevious())
            assertEquals(0, it.nextIndex())
            assertEquals(-1, it.previousIndex())
            assertFailsWith<NoSuchElementException> { it.next() }
            assertFailsWith<NoSuchElementException> { it.previous() }
        }

        testListIterator(ref)
        testListIterator(impl)
    }

    @Test
    fun testNonEmpty() {
        // TODO
    }
}
