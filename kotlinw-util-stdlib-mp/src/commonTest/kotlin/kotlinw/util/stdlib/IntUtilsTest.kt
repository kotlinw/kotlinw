package kotlinw.util.stdlib

import kotlin.test.Test
import kotlin.test.assertEquals

class IntUtilsTest {

    @Test
    fun testWriteToReadFromByteArray() {
        testWriteToReadFromByteArray(0)
        testWriteToReadFromByteArray(1)
        testWriteToReadFromByteArray(-1)
        testWriteToReadFromByteArray(Int.MIN_VALUE)
        testWriteToReadFromByteArray(Int.MAX_VALUE)
        testWriteToReadFromByteArray(255)
        testWriteToReadFromByteArray(256)
        testWriteToReadFromByteArray(1_000_000_000)
    }

    private fun testWriteToReadFromByteArray(i: Int) {
        val buffer = ByteArray(Int.SIZE_BYTES)
        i.writeToByteArray(buffer, 0)
        assertEquals(i, Int.readFromByteArray(buffer, 0))
    }
}
