package kotlinw.util

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class Url(val value: String) {
    override fun toString() = value
}

fun Url.trailingPathSeparatorRemoved() = if (value.endsWith("/")) Url(value.substring(0, value.length - 1)) else this

fun Url.withTrailingPathSeparator() = if (value.endsWith("/")) this else Url("$value/")

operator fun Url.plus(path: String) = Url(value + path) // TODO kezelni a trailing /-t
