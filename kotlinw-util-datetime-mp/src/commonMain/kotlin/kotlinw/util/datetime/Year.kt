package kotlinw.util.datetime

import kotlinx.datetime.Month
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Year(val value: Int)

expect fun isLeapYear(year: Int): Boolean

val Year.isLeapYear: Boolean get() = isLeapYear(value)

fun Year.atMonth(month: Month) = YearMonth(this, month)

fun Year.atMonth(monthNumber: Int) = YearMonth(this, Month(monthNumber))
