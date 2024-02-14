package kotlinw.logging.js.console

import kotlinw.logging.api.LogEntryAttribute
import kotlinw.logging.api.LogLevel
import kotlinw.logging.api.LogLevel.Debug
import kotlinw.logging.api.LogLevel.Info
import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.Logger
import kotlinw.logging.spi.LoggerImplementor
import kotlinw.logging.spi.LoggingIntegrator

object ConsoleLoggingIntegrator: LoggingIntegrator {

    private value class ConsoleLogger(override val name: String) : LoggerImplementor {

        override val loggingIntegrator: LoggingIntegrator get() = ConsoleLoggingIntegrator
    }

    override fun getLogger(loggerName: String): Logger = ConsoleLogger(loggerName)

    override fun isLogLevelEnabled(logger: LoggerImplementor, level: LogLevel): Boolean = level >= Info // TODO konfig

    override fun log(
        logger: LoggerImplementor,
        level: LogLevel,
        cause: Throwable?,
        message: LogMessage,
        attributes: Collection<LogEntryAttribute>
    ) {
        if (isLogLevelEnabled(logger, level)) {
            console.log(message.toString()) // TODO
        }
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Not supported on JS platform", level = DeprecationLevel.ERROR)
    override fun <T> withNonSuspendableLoggingContext(contextChangeMap: Map<String, String?>, block: () -> T) =
        throw UnsupportedOperationException()

    override suspend fun <T> withLoggingContext(contextChangeMap: Map<String, String?>, block: suspend () -> T): T =
        block() // TODO implement
}
