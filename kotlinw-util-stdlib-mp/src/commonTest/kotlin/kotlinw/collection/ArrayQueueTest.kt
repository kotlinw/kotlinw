package kotlinw.collection

import kotlin.test.Test

class ArrayQueueTest {

    private val testSuite = MutableQueueTestSuite(
        newTestedQueue = { ArrayQueue() },
        newReferenceImplementationQueue = { SimpleArrayListQueue() }
    )

    @Test
    fun testSuite() {
        testSuite.test()
    }
}
