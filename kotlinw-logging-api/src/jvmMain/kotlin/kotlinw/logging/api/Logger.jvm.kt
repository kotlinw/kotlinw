package kotlinw.logging.api

import kotlinw.logging.spi.LoggerImplementor

actual fun <T> Logger.withNonSuspendableLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: () -> T
): T =
    (this as LoggerImplementor).loggingIntegrator.withNonSuspendableLoggingContext(contextChangeMap, block)
