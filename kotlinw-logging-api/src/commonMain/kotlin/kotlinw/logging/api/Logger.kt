@file:JvmName("LoggingContextJvm")
package kotlinw.logging.api

import kotlin.jvm.JvmName
import kotlinw.logging.spi.LogMessageProvider
import kotlinw.logging.spi.LoggerImplementor
import kotlinw.logging.spi.buildLogMessage

interface Logger {

    val name: String

    val isTraceEnabled get() = isLogLevelEnabled(LogLevel.Trace)

    val isDebugEnabled get() = isLogLevelEnabled(LogLevel.Debug)

    val isInfoEnabled get() = isLogLevelEnabled(LogLevel.Info)

    val isWarningEnabled get() = isLogLevelEnabled(LogLevel.Warning)

    val isErrorEnabled get() = isLogLevelEnabled(LogLevel.Error)

    fun trace(cause: Throwable? = null, messageProvider: LogMessageProvider) = log(LogLevel.Trace,cause, messageProvider)

    fun debug(cause: Throwable? = null, messageProvider: LogMessageProvider) = log(LogLevel.Debug,cause, messageProvider)

    fun info(cause: Throwable? = null, messageProvider: LogMessageProvider) = log(LogLevel.Info,cause, messageProvider)

    fun warning(cause: Throwable? = null, messageProvider: LogMessageProvider) = log(LogLevel.Warning,cause,messageProvider)

    fun error(cause: Throwable? = null, messageProvider: LogMessageProvider) = log(LogLevel.Error,cause, messageProvider)
}

fun Logger.log(logLevel: LogLevel, cause: Throwable? = null, messageProvider: LogMessageProvider) {
    log(logLevel, cause, buildLogMessage(messageProvider), emptyList())
}

fun Logger.isLogLevelEnabled(level: LogLevel) = (this as LoggerImplementor).loggingIntegrator.isLogLevelEnabled(this, level)

internal fun Logger.log(
    level: LogLevel,
    cause: Throwable?,
    message: LogMessage,
    attributes: Collection<LogEntryAttribute>
) =
    (this as LoggerImplementor).loggingIntegrator.log(this, level, cause, message, attributes)

expect fun <T> Logger.withNonSuspendableLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: () -> T
): T

suspend fun <T> Logger.withLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: suspend () -> T
): T =
    (this as LoggerImplementor).loggingIntegrator.withLoggingContext(contextChangeMap, block)
