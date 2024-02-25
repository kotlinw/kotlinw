package kotlinw.util.stdlib

enum class ByteSizeUnit(val degree: Int) {
    B(0),
    KB(3),
    MB(6),
    GB(9),
    TB(12),
    PB(15),
    EB(18),
    ZB(21),
    YB(24),
    RB(27),
    QB(30)
}

data class ByteSize(val size: Double, val unit: ByteSizeUnit) {

    companion object {

        fun formatByteSize(
            size: Double,
            unit: ByteSizeUnit,
            maxFractionalDigits: Int = 0
        ) =
            "${size.format(maxFractionalDigits)} $unit"
    }

    fun format(maxFractionalDigits: Int = 0): String =
        formatByteSize(size, unit, maxFractionalDigits)
}

fun Double.roundToByteSize(unit: ByteSizeUnit? = null): ByteSize {
    var unitIndex = 0
    var size = this@roundToByteSize

    if (unit != null) {
        while (unitIndex <= ByteSizeUnit.entries.lastIndex && ByteSizeUnit.entries[unitIndex] < unit) {
            size /= 1024.0
            unitIndex++
        }
    } else {
        while (size >= 1024.0 && unitIndex < ByteSizeUnit.entries.lastIndex) {
            size /= 1024.0
            unitIndex++
        }
    }
    return ByteSize(size, ByteSizeUnit.entries[unitIndex])
}

fun Number.roundToByteSize(unit: ByteSizeUnit? = null): ByteSize = toDouble().roundToByteSize(unit)

fun Double.formatByteSize(
    unit: ByteSizeUnit? = null,
    maxFractionalDigits: Int = 0
): String = roundToByteSize(unit).format(maxFractionalDigits)

fun Number.formatByteSize(
    unit: ByteSizeUnit? = null,
    maxFractionalDigits: Int = 0
): String =
    toDouble().formatByteSize(unit, maxFractionalDigits)

fun Double.format(
    maxFractionalDigits: Int,
    minFractionDigits: Int = maxFractionalDigits,
    useDecimalGrouping: Boolean = true
): String {
    require(maxFractionalDigits >= 0)
    require(minFractionDigits <= maxFractionalDigits)

    val (integerPart, fractionalPart) =
        round(maxFractionalDigits).toString() // TODO handle scientific notation
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
            val fractionalDigits = if (fractionalDigitCount in minFractionDigits..maxFractionalDigits) {
                fractionalPart
            } else if (fractionalDigitCount < minFractionDigits) {
                fractionalPart + "0".repeat(minFractionDigits - fractionalDigitCount)
            } else {
                fractionalPart.dropLast(fractionalDigitCount - maxFractionalDigits)
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

fun Float.format(maxFractionalDigits: Int, minFractionDigits: Int): String =
    toDouble().format(maxFractionalDigits, minFractionDigits)

fun Int.format(useDecimalGrouping: Boolean = true) =
    formatInt(this@format.toString(), useDecimalGrouping)

private fun formatInt(intAsString: String, useDecimalGrouping: Boolean) =
    if (useDecimalGrouping)
        intAsString.reversed().chunked(3).joinToString(" ").reversed()
    else
        intAsString
