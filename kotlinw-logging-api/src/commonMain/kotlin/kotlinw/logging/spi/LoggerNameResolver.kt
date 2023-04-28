@file:JvmName("LoggerNameResolverJvm")
package kotlinw.logging.spi

import kotlin.jvm.JvmName

@PublishedApi
internal const val UnknownLoggerName = "<Unknown>"

@PublishedApi
internal expect inline fun resolveLoggerName(noinline function: () -> Unit): String?
