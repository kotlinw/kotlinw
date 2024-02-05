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

        assertEquals("32 KB", 3300330.formatByteSize(ByteSizeUnit.B))
    }

    @Test
    fun testFloatFormat() {
        assertEquals("0", 0.0.format(0))
        assertEquals("0", 0.0.format(0))

        assertEquals("0.00", 0.0.format(2))
        assertEquals("0.11", 0.111.format(2))
        assertEquals("0.11", 0.11111.format(2))

        assertEquals("0.000", 0.0.format(3))
        assertEquals("0.00000", 0.0.format(5))
    }

    @Test
    fun testFloatFormat2() {
        assertEquals("0", 0.001.format(2, 0))
        assertEquals("0.0", 0.001.format(2, 1))
        assertEquals("0.000", 0.0.format(5, 3))

        assertEquals("0.10", 0.1.format(3, 2))
        assertEquals("0.11", 0.11111.format(2, 2))
        assertEquals("0.1111", 0.11111.format(4, 2))
        assertEquals("0.11111", 0.11111.format(6, 2))
    }

    @Test
    fun testFormatIntWithDecimalGrouping() {
        assertEquals("0", 0.formatWithDecimalGrouping())
        assertEquals("100", 100.formatWithDecimalGrouping())
        assertEquals("1 000", 1_000.formatWithDecimalGrouping())
        assertEquals("10 000", 10_000.formatWithDecimalGrouping())
        assertEquals("1 000 000", 1_000_000.formatWithDecimalGrouping())
    }
}
