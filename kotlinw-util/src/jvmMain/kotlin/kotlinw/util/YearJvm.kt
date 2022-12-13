package kotlinw.util

import java.time.Year

actual fun isLeapYear(year: Int): Boolean = Year.isLeap(year.toLong())
