package kotlinw.remoting.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class ServiceLocator(val serviceId: String, val methodId: String)
