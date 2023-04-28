package kotlinw.logging.stdout

import kotlinw.logging.api.LogEntryAttribute
import kotlinw.logging.api.LogLevel
import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.Logger
import kotlinw.logging.spi.LoggerImplementor
import kotlinw.logging.spi.LoggingIntegrator
import kotlinx.datetime.Clock
import kotlin.jvm.JvmInline

object StdoutLoggingIntegrator : LoggingIntegrator {

    @JvmInline
    private value class StdoutLogger(override val name: String) : LoggerImplementor {

        override val loggingIntegrator: LoggingIntegrator get() = StdoutLoggingIntegrator
    }

    override fun getLogger(loggerName: String): Logger = StdoutLogger(loggerName)

    override fun isLogLevelEnabled(logger: LoggerImplementor, level: LogLevel): Boolean = true

    override fun <T> withLoggingContext(contextChangeMap: Map<String, String?>, block: () -> T): T =
        block() // TODO implement

    override suspend fun <T> withCoroutineLoggingContext(
        contextChangeMap: Map<String, String?>,
        block: suspend () -> T
    ): T =
        block() // TODO implement

    override fun log(
        logger: LoggerImplementor,
        level: LogLevel,
        cause: Throwable?,
        message: LogMessage,
        attributes: Collection<LogEntryAttribute>
    ) {
        val timestamp = Clock.System.now()
        println("$timestamp [${logger.name}] ${level.conventionalName} $message")
        cause?.printStackTrace()
    }
}
