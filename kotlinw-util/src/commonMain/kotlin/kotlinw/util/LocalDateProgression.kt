package kotlinw.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class LocalDateProgression internal constructor(
    override val start: LocalDate,
    override val endInclusive: LocalDate,
    internal val step: DatePeriod = DatePeriod(days = 1)
) : Iterable<LocalDate>, ClosedRange<LocalDate> {

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

operator fun LocalDate.rangeTo(other: LocalDate): LocalDateProgression = LocalDateProgression(this, other)

fun ClosedRange<LocalDate>.toLocalDateProgression(step: DatePeriod = DatePeriod(days = 1)) =
    if (this is LocalDateProgression && this.step == step)
        this
    else
        LocalDateProgression(start, endInclusive, step)
