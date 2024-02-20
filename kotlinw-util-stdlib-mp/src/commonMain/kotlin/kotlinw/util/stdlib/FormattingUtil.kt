package kotlinw.util.stdlib

enum class ByteSizeUnit {
    B, KB, MB, GB, TB, PB, EB, ZB
}

data class ByteSize(val size: Int, val unit: ByteSizeUnit) {

    companion object {

        fun formatByteSize(size: Int, unit: ByteSizeUnit) = "${size.format()} $unit"
    }

    fun format(): String = formatByteSize(size, unit)
}

fun Long.roundToByteSize(unit: ByteSizeUnit? = null): ByteSize {
    var unitIndex = 0
    var size = this@roundToByteSize

    while (size >= 1024L && unitIndex <= ByteSizeUnit.entries.lastIndex && (unit == null || ByteSizeUnit.entries[unitIndex] < unit)) {
        size = size.shr(10)
        unitIndex++
    }
    return ByteSize(size.toInt(), ByteSizeUnit.entries[unitIndex])
}

fun Number.roundToByteSize(unit: ByteSizeUnit? = null): ByteSize = toLong().roundToByteSize(unit)

fun Long.formatByteSize(unit: ByteSizeUnit? = null): String = roundToByteSize(unit).format()

fun Number.formatByteSize(unit: ByteSizeUnit? = null): String = toLong().formatByteSize(unit)

fun Double.format(
    maxFractionDigits: Int,
    minFractionDigits: Int = maxFractionDigits,
    useDecimalGrouping: Boolean = true
): String {
    require(maxFractionDigits >= 0)
    require(minFractionDigits <= maxFractionDigits)

    val (integerPart, fractionalPart) =
        round(maxFractionDigits).toString() // TODO handle scientific notation
            .split('.').let {
                it[0] to (if (it.size > 1 && it[1].any { it != '0' }) it[1].dropLastWhile { it == '0' } else null)
            }
    return buildString {
        append(formatInt(integerPart, useDecimalGrouping))

        if (fractionalPart == null || minFractionDigits == 0) {
            if (minFractionDigits > 0) {
                append('.')
                repeat(minFractionDigits) {
                    if (useDecimalGrouping && it > 0 && it % 3 == 0) {
                        append(' ')
                    }
                    append('0')
                }
            }
        } else {
            append('.')

            val fractionalDigitCount = fractionalPart.length
            val fractionalDigits = if (fractionalDigitCount in minFractionDigits..maxFractionDigits) {
                fractionalPart
            } else if (fractionalDigitCount < minFractionDigits) {
                fractionalPart + "0".repeat(minFractionDigits - fractionalDigitCount)
            } else {
                fractionalPart.dropLast(fractionalDigitCount - maxFractionDigits)
            }

            append(
                if (useDecimalGrouping) {
                    fractionalDigits.chunked(3).joinToString(" ")
                } else {
                    fractionalDigits
                }
            )
        }
    }
}

fun Float.format(fractionDigits: Int) = format(fractionDigits, fractionDigits)

fun Float.format(maxFractionDigits: Int, minFractionDigits: Int): String =
    toDouble().format(maxFractionDigits, minFractionDigits)

fun Int.format(useDecimalGrouping: Boolean = true) =
    formatInt(this@format.toString(), useDecimalGrouping)

private fun formatInt(intAsString: String, useDecimalGrouping: Boolean) =
    if (useDecimalGrouping)
        intAsString.reversed().chunked(3).joinToString(" ").reversed()
    else
        intAsString
