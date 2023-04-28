package kotlinw.logging.api

import kotlinw.logging.spi.LoggerImplementor

interface Logger {

    val name: String
}

internal fun Logger.isLogLevelEnabled(level: LogLevel) = (this as LoggerImplementor).loggingIntegrator.isLogLevelEnabled(this, level)

internal fun Logger.log(
    level: LogLevel,
    cause: Throwable?,
    message: LogMessage,
    attributes: Collection<LogEntryAttribute>
) =
    (this as LoggerImplementor).loggingIntegrator.log(this, level, cause, message, attributes)

val Logger.isTraceEnabled get() = isLogLevelEnabled(LogLevel.Trace)

val Logger.isDebugEnabled get() = isLogLevelEnabled(LogLevel.Debug)

val Logger.isInfoEnabled get() = isLogLevelEnabled(LogLevel.Info)

val Logger.isWarningEnabled get() = isLogLevelEnabled(LogLevel.Warning)

val Logger.isErrorEnabled get() = isLogLevelEnabled(LogLevel.Error)
