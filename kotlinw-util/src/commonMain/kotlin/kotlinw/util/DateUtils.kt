package kotlinw.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// Source: https://github.com/Kotlin/kotlinx-datetime/issues/74
/**
 * Returns this date with another day of month.
 *
 * @throws IllegalArgumentException if the resulting date is invalid or exceeds the platform-specific boundary.
 */
fun LocalDate.withDayOfMonth(dayOfMonth: Int): LocalDate = LocalDate(this.year, this.month, dayOfMonth)

// Source: https://github.com/Kotlin/kotlinx-datetime/issues/74
/**
 * The beginning of the next month.
 */
val LocalDate.firstDayOfNextMonth
    get() = withDayOfMonth(1).plus(1, DateTimeUnit.MONTH)

// Source: https://github.com/Kotlin/kotlinx-datetime/issues/74
val LocalDate.atEndOfMonth get() = firstDayOfNextMonth - DatePeriod(days = 1)

val LocalDate.atStartOfMonth get() = withDayOfMonth(1)
