@file:JvmName("LoggingContextJvm")
package kotlinw.logging.platform

import kotlin.jvm.JvmName

expect fun <T> withLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: () -> T
): T


suspend fun <T> withCoroutineLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: suspend () -> T
): T =
    PlatformLogging.withCoroutineLoggingContext(contextChangeMap, block)
