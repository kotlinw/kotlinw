package kotlinw.util

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class CountryCode(val code: String) {

    override fun toString(): String = code
}
