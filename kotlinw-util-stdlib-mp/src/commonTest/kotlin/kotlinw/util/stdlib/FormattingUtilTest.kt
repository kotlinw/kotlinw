package kotlinw.util.stdlib

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattingUtilTest {

    @Test
    fun testFormatByteSize() {
        assertEquals("0 B", 0.formatByteSize())
        assertEquals("1 B", 1.formatByteSize())
        assertEquals("1023 B", 1023.formatByteSize())
        assertEquals("1 KB", 1024.formatByteSize())
        assertEquals("32 KB", 33000.formatByteSize())
    }
}
