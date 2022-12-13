package kotlinw.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class LocalDateProgression(
    private val start: LocalDate,
    private val endInclusive: LocalDate,
    private val step: DatePeriod = DatePeriod(days = 1)
) : Iterable<LocalDate> {
    override fun iterator(): Iterator<LocalDate> =
        object : Iterator<LocalDate> {
            var nextValue: LocalDate = start

            override fun hasNext(): Boolean = nextValue <= endInclusive

            override fun next(): LocalDate {
                val next = nextValue
                nextValue = nextValue.plus(step)
                return next
            }
        }
}
