package kotlinw.util

fun UByte.toHexString(padded: Boolean = false) = if (padded) toString(16).padStart(UByte.SIZE_BITS / 4, '0') else toString(16)

fun Byte.toHexString(padded: Boolean = false) = toUByte().toHexString(padded)

fun UShort.toHexString(padded: Boolean = false) = if (padded) toString(16).padStart(UShort.SIZE_BITS / 4, '0') else toString(16)

fun Short.toHexString(padded: Boolean = false) = toUShort().toHexString(padded)

fun UInt.toHexString(padded: Boolean = false) = if (padded) toString(16).padStart(UInt.SIZE_BITS / 4, '0') else toString(16)

fun Int.toHexString(padded: Boolean = false) = toUInt().toHexString(padded)

fun ULong.toHexString(padded: Boolean = false) = if (padded) toString(16).padStart(ULong.SIZE_BITS / 4, '0') else toString(16)

fun Long.toHexString(padded: Boolean = false) = toULong().toHexString(padded)
