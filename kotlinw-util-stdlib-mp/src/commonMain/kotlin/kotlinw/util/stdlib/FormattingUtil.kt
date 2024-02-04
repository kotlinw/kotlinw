package kotlinw.util.stdlib

private val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB")

fun ULong.formatByteSize(): String {
    var unit = 0
    var size = this@formatByteSize

    while (size >= 1024UL && unit < units.size - 1) {
        size = size.shr(10)
        unit++
    }
    return "$size ${units[unit]}"
}

fun Number.formatByteSize(): String = toLong().toULong().formatByteSize()
