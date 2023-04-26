package kotlinw.remoting.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ServiceLocator(val id: String)
