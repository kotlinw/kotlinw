package kotlinw.util

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Priority(val value: Int) : Comparable<Priority> {

    companion object {

        val Normal = Priority(0)
    }

    override fun compareTo(other: Priority): Int = value.compareTo(other.value)
}

interface HasPriority {

    val priority: Priority
}
