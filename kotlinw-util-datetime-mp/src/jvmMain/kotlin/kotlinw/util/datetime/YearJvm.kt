package kotlinw.util.datetime

import java.time.Year

actual fun isLeapYear(year: Int): Boolean = Year.isLeap(year.toLong())
