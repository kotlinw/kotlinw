package kotlinw.util

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Priority(val value: Int) : Comparable<Priority> {

    companion object {

        val Lowest = Priority(Int.MIN_VALUE)

        val Normal = Priority(0)

        val Highest = Priority(Int.MAX_VALUE)
    }

    override fun compareTo(other: Priority): Int = value.compareTo(other.value)
}

interface HasPriority {

    companion object {

        val comparator = Comparator<HasPriority> { a, b -> a.priority.compareTo(b.priority) }
    }

    val priority: Priority
}
