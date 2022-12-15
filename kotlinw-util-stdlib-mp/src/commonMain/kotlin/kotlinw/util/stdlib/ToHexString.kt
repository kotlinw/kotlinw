package kotlinw.util.stdlib

private fun String.padStartIfNeeded(padded: Boolean, padLength: Int) = if (padded) padStart(padLength, '0') else this

fun UByte.toHexString(padded: Boolean = false) = toString(16).padStartIfNeeded(padded, UByte.SIZE_BITS / 4)

fun Byte.toHexString(padded: Boolean = false) = toUByte().toHexString(padded)

fun UShort.toHexString(padded: Boolean = false) = toString(16).padStartIfNeeded(padded, UShort.SIZE_BITS / 4)

fun Short.toHexString(padded: Boolean = false) = toUShort().toHexString(padded)

fun UInt.toHexString(padded: Boolean = false) = toString(16).padStartIfNeeded(padded, UInt.SIZE_BITS / 4)

fun Int.toHexString(padded: Boolean = false) = toUInt().toHexString(padded)

fun ULong.toHexString(padded: Boolean = false) = toString(16).padStartIfNeeded(padded, ULong.SIZE_BITS / 4)

fun Long.toHexString(padded: Boolean = false) = toULong().toHexString(padded)
