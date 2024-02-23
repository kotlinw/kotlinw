package kotlinw.util.stdlib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.util.stdlib.ByteSizeUnit.B
import kotlinw.util.stdlib.ByteSizeUnit.GB
import kotlinw.util.stdlib.ByteSizeUnit.KB
import kotlinw.util.stdlib.ByteSizeUnit.MB

class FormattingUtilTest {

    @Test
    fun testFormatByteSize() {
        assertEquals("0 B", 0.formatByteSize())
        assertEquals("1 B", 1.formatByteSize())
        assertEquals("1 023 B", 1023.formatByteSize())
        assertEquals("1 KB", 1024.formatByteSize())
        assertEquals("32.2 KB", 33000.formatByteSize(maxFractionalDigits = 1))
        assertEquals("32 KB", 33000.formatByteSize())
        assertEquals("33 000 B", 33000.formatByteSize(B))
        assertEquals("3 300 330 B", 3_300_330.formatByteSize(B))
        assertEquals("3 223 KB", 3_300_330.formatByteSize(KB))
        assertEquals("3 MB", 3_300_330.formatByteSize())
        assertEquals("3 MB", 3_300_330.formatByteSize(MB))
        assertEquals("3.1 MB", 3_300_330.formatByteSize(unit = MB, maxFractionalDigits = 1))
        assertEquals("3.15 MB", 3_300_330.formatByteSize(unit = MB, maxFractionalDigits = 2))
        assertEquals("0.003 GB", 3_300_330.formatByteSize(unit = GB, maxFractionalDigits = 3))
    }

    @Test
    fun testFloatFormat() {
        assertEquals("0", 0.0.format(0))
        assertEquals("0.0", 0.0.format(1))
        assertEquals("0.00", 0.0.format(2))
        assertEquals("0.11", 0.111.format(2))
        assertEquals("0.11", 0.11111.format(2))
        assertEquals("0.000", 0.0.format(3))
        assertEquals("0.000 00", 0.0.format(5))
        assertEquals("11 111.111 11", 11111.11111.format(5))
    }

    @Test
    fun testFloatFormat2() {
        assertEquals("0", 0.001.format(2, 0))
        assertEquals("0.0", 0.001.format(2, 1))
        assertEquals("0.000", 0.0.format(5, 3))
        assertEquals("0.10", 0.1.format(3, 2))
        assertEquals("0.11", 0.11111.format(2, 2))
        assertEquals("0.111 1", 0.11111.format(4, 2))
        assertEquals("0.111 11", 0.11111.format(6, 2))
    }

    @Test
    fun testFormatIntWithDecimalGrouping() {
        assertEquals("0", 0.format())
        assertEquals("100", 100.format())
        assertEquals("1 000", 1_000.format())
        assertEquals("10 000", 10_000.format())
        assertEquals("1 000 000", 1_000_000.format())
    }
}
