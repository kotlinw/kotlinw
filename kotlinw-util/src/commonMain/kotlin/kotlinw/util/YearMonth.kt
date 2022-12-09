package kotlinw.util

import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlinx.serialization.Serializable

@Serializable
data class YearMonth(val year: Int, val monthNumber: Int) {
    constructor(year: Int, month: Month) : this(year, month.number)

    override fun toString() = formatIso()
}

fun YearMonth.formatIso() = year.toString() + "-" + monthNumber.toString().padStart(2, '0')
