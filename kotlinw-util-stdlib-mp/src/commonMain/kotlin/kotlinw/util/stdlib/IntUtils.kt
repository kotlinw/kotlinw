package kotlinw.util.stdlib

fun Int.writeToByteArray(target: ByteArray, offset: Int) {
    require(target.size >= offset + Int.SIZE_BYTES)
    target[offset] = (this ushr 24 and 0xff).toByte()
    target[offset + 1] = (this ushr 16 and 0xff).toByte()
    target[offset + 2] = (this ushr 8 and 0xff).toByte()
    target[offset + 3] = (this and 0xff).toByte()
}

fun Int.Companion.readFromByteArray(source: ByteArray, offset: Int): Int {
    require(source.size >= offset + SIZE_BYTES)
    return ((source[offset].toInt() and 0xff) shl 24) or
            ((source[offset + 1].toInt() and 0xff) shl 16) or
            ((source[offset + 2].toInt() and 0xff) shl 8) or
            (source[offset + 3].toInt() and 0xff)
}

fun Int.Companion.readFromByteArrayView(source: ByteArrayView, offset: Int): Int {
    require(source.size >= offset + SIZE_BYTES)
    return ((source[offset].toInt() and 0xff) shl 24) or
            ((source[offset + 1].toInt() and 0xff) shl 16) or
            ((source[offset + 2].toInt() and 0xff) shl 8) or
            (source[offset + 3].toInt() and 0xff)
}
