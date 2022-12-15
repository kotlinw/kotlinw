package kotlinw.util.stdlib

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class OpaqueValue(val value: String)
