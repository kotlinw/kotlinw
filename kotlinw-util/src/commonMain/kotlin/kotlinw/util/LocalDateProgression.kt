package kotlinw.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class LocalDateProgression internal constructor(
    override val start: LocalDate,
    override val endInclusive: LocalDate,
    internal val step: DatePeriod = defaultStep
) : Iterable<LocalDate>, ClosedRange<LocalDate>, HasDisplayName {

    companion object {

        val defaultStep = DatePeriod(days = 1)
    }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalDateProgression) return false

        if (start != other.start) return false
        if (endInclusive != other.endInclusive) return false
        if (step != other.step) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + endInclusive.hashCode()
        result = 31 * result + step.hashCode()
        return result
    }

    override fun toString(): String {
        return "LocalDateProgression(start=$start, endInclusive=$endInclusive, step=$step)"
    }

    override val displayName: String
        get() = buildString {
            append(start)
            append(" - ")
            append(endInclusive)

            if (step != defaultStep) {
                append("(with steps of ")
                append(step)
                append(")")
            }
        }
}

operator fun LocalDate.rangeTo(other: LocalDate): LocalDateProgression = LocalDateProgression(this, other)

fun ClosedRange<LocalDate>.toLocalDateProgression(step: DatePeriod = DatePeriod(days = 1)) =
    if (this is LocalDateProgression && this.step == step)
        this
    else
        LocalDateProgression(start, endInclusive, step)
