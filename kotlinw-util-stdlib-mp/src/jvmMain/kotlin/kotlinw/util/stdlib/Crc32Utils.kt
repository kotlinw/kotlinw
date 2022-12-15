package kotlinw.util.stdlib

import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream

fun InputStream.crc32(): Long {
    val checkedInputStream = CheckedInputStream(this.buffered(), CRC32())

    @Suppress("ControlFlowWithEmptyBody")
    while (checkedInputStream.read() >= 0) {
    }

    return checkedInputStream.checksum.value
}
