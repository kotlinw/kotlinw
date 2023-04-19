package kotlinw.collection

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkedQueueTest {

    private val testSuite = MutableQueueTestSuite(
        newTestedQueue = { LinkedQueue() },
        newReferenceImplementationQueue = { SimpleArrayListQueue() }
    )

    @Test
    fun testSuite() {
        testSuite.test()
    }

    @Test
    fun testMutableIteratorBug() {
        LinkedQueue<Int>().apply {
            addAll(1..10)

            iterator().apply {
                this as LinkedQueue.MutableIteratorImpl

                assertEquals(null, previousNode)
                assertEquals(null, currentNode)
                assertEquals(1, nextNode!!.value)
                assertEquals(1, headNode!!.value)

                assertEquals(1, next()) // 1

                assertEquals(null, previousNode)
                assertEquals(1, currentNode!!.value)
                assertEquals(2, nextNode!!.value)
                assertEquals(1, headNode!!.value)

                remove()

                assertEquals(null, previousNode)
                assertEquals(null, currentNode)
                assertEquals(2, nextNode!!.value)
                assertEquals(2, headNode!!.value)

                assertEquals(2, next()) // 2

                assertEquals(null, previousNode)
                assertEquals(2, currentNode!!.value)
                assertEquals(3, nextNode!!.value)
                assertEquals(2, headNode!!.value)

                assertEquals(3, next()) // 3
                remove()

                assertEquals(4, next()) // 4
                remove()

                assertEquals(5, next()) // 5
                remove()

                assertEquals(6, next()) // 6
                assertEquals(7, next()) // 7
                assertEquals(8, next()) // 8
                assertEquals(9, next()) // 9
                assertEquals(10, next()) // 10
            }

            assertEquals(listOf(2, 6, 7, 8, 9, 10), toList())
        }
    }
}
