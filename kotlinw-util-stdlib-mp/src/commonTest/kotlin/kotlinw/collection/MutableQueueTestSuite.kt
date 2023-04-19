package kotlinw.collection

import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableQueueTestSuite(
    private val newTestedQueue: () -> MutableQueue<Int>,
    private val newReferenceImplementationQueue: () -> MutableQueue<Int>
) {
    fun test() {
        testEmpty()
        testEnqueue()
        testEnqueueDequeue()
        testOperationsAgainstReferenceImplementation()
        testMutableIteratorAgainstReferenceImplementation()
        testMutableIterator()
    }

    private fun testEmpty() {
        val empty = newTestedQueue()
        assertEquals(0, empty.size)
        assertEquals(null, empty.peekOrNull())
        assertEquals(null, empty.dequeueOrNull())
        assertFalse(empty.iterator().hasNext())
    }

    private fun testEnqueue() {
        val queue = newTestedQueue()

        for (i in 1..5) {
            queue.enqueue(i)

            assertEquals(i, queue.size)
            assertEquals(1, queue.peekOrNull())
            assertEquals((1..i).toList(), queue.toList())
        }
    }

    private fun testEnqueueDequeue() {
        newTestedQueue().apply {
            enqueue(1)
            assertEquals(1, dequeueOrNull())
            assertTrue(isEmpty())
        }

        for (i in 1..5) {
            newTestedQueue().apply {
                repeat(i) {
                    enqueue(it)
                    assertTrue(isNotEmpty())
                }

                repeat(i) {
                    dequeueOrNull()
                }

                assertTrue(isEmpty())
            }
        }
    }

    private fun testOperationsAgainstReferenceImplementation() {
        val testedQueue = newTestedQueue()
        val referenceQueue = newReferenceImplementationQueue()

        val random = Random.Default

        var i = 0;
        do {
            assertEquals(referenceQueue.toList(), testedQueue.toList())
            assertEquals(referenceQueue.peekOrNull(), testedQueue.peekOrNull())

            if (random.nextInt(10) < 6) {
                testedQueue.enqueue(i)
                referenceQueue.enqueue(i)
            } else {
                testedQueue.dequeueOrNull()
                referenceQueue.dequeueOrNull()
            }
        } while (i++ < 1000)
    }

    private fun testMutableIteratorAgainstReferenceImplementation() {
        val random = Random.Default
        val elements = (1..1000).toList()

        val referenceQueue = newReferenceImplementationQueue().apply { addAll(elements) }
        val testedQueue = newTestedQueue().apply { addAll(elements) }

        assertEquals(elements, referenceQueue.toList())
        assertEquals(elements, testedQueue.toList())

        val referenceIterator = referenceQueue.iterator()
        val testedIterator = testedQueue.iterator()

        for (i in 1..elements.size / 2) {
            val referenceNext = referenceIterator.next()
            val testedNext = testedIterator.next()

            assertEquals(referenceNext, testedNext)

            if (random.nextBoolean()) {
                referenceIterator.remove()
                testedIterator.remove()
            }
        }

        assertEquals(referenceQueue.toList(), testedQueue.toList())
    }

    private fun testMutableIterator() {
        newTestedQueue().apply {
            iterator().apply {
                assertFalse(hasNext())
                assertFailsWith<NoSuchElementException> { next() }
            }
        }

        val queueSize = 100
        for (i in 1..1000) {
            newTestedQueue().apply {
                addAll(1..queueSize)

                iterator().apply {
                    while (hasNext()) {
                        val next = next()
                        if (next == i) {
                            remove()
                        }
                    }
                }

                assertEquals((1..queueSize).filter { it != i }.toList(), toList())
            }
        }
    }
}
