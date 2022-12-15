package kotlinw.util.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlinx.serialization.Serializable

@Serializable
data class YearMonth(val year: Year, val month: Month) {

    companion object {

        fun of(year: Int, month: Month) = YearMonth(Year(year), month)
    }

    override fun toString() = formatIso()
}

fun YearMonth.formatIso() = year.toString() + "-" + month.number.toString().padStart(2, '0')

val YearMonth.firstDay get() = atDayOfMonth(1)

val YearMonth.lastDay get() = firstDay.atEndOfMonth

val YearMonth.daysInMonth get() = lastDay.dayOfMonth

fun YearMonth.atDayOfMonth(dayOfMonth: Int) = LocalDate(year.value, month, dayOfMonth)
