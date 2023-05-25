package kotlinw.util.stdlib

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Priority(val value: Int) : Comparable<Priority> {

    companion object {

        val Lowest = Priority(Int.MAX_VALUE)

        val Normal = Priority(0)

        val Highest = Priority(Int.MIN_VALUE)

        fun Priority.lowerBy(value: Int) = Priority(this.value + 1)

        fun Priority.higherBy(value: Int) = Priority(this.value - 1)
    }

    override fun compareTo(other: Priority): Int = value.compareTo(other.value)
}

interface HasPriority {

    companion object {

        val comparator = Comparator<HasPriority> { a, b -> a.priority.compareTo(b.priority) }
    }

    val priority: Priority
}

fun <T> Iterable<T>.sortedByPriority(defaultPriority: Priority = Priority.Normal) =
    sortedBy {
        if (it is HasPriority) {
            it.priority
        } else {
            defaultPriority
        }
    }
