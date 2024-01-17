package kotlinw.logging.slf4j

import kotlinw.logging.api.LogEntryAttribute
import kotlinw.logging.api.LogLevel
import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.Logger
import kotlinw.logging.api.SimpleMarker
import kotlinw.logging.spi.LoggerImplementor
import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.logging.spi.processPlaceholders
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.MarkerFactory
import org.slf4j.Logger as Slf4jLogger
import org.slf4j.event.Level as Slf4jLevel

class Slf4jLoggingIntegrator : LoggingIntegrator {

    companion object {

        private const val ARGUMENT_PLACEHOLDER = "{}"
    }

    inner class Slf4jLoggerWrapper(val slf4jLogger: Slf4jLogger) : LoggerImplementor {

        override val loggingIntegrator: LoggingIntegrator get() = this@Slf4jLoggingIntegrator

        override val name: String get() = slf4jLogger.name
    }

    override fun getLogger(loggerName: String): Logger = Slf4jLoggerWrapper(LoggerFactory.getLogger(loggerName))

    override fun isLogLevelEnabled(logger: LoggerImplementor, level: LogLevel): Boolean {
        check(logger is Slf4jLoggerWrapper)
        return with(logger.slf4jLogger) {
            when (level) {
                LogLevel.Trace -> isTraceEnabled
                LogLevel.Debug -> isDebugEnabled
                LogLevel.Info -> isInfoEnabled
                LogLevel.Warning -> isWarnEnabled
                LogLevel.Error -> isErrorEnabled
            }
        }
    }

    private fun LogLevel.asSlf4jLevel(): Slf4jLevel =
        when (this) {
            LogLevel.Error -> Slf4jLevel.ERROR
            LogLevel.Warning -> Slf4jLevel.WARN
            LogLevel.Info -> Slf4jLevel.INFO
            LogLevel.Debug -> Slf4jLevel.DEBUG
            LogLevel.Trace -> Slf4jLevel.TRACE
        }

    override fun log(
        logger: LoggerImplementor,
        level: LogLevel,
        cause: Throwable?,
        message: LogMessage,
        attributes: Collection<LogEntryAttribute>
    ) {
        check(logger is Slf4jLoggerWrapper)

        val slf4jLogger = logger.slf4jLogger
        val slf4jLevel = level.asSlf4jLevel()

        if (!slf4jLogger.isEnabledForLevel(slf4jLevel)) {
            return
        }

        val builder = slf4jLogger.makeLoggingEventBuilder(slf4jLevel)

        if (cause != null) {
            builder.setCause(cause)
        }

        for (attribute in attributes) {
            when (attribute) {
                is SimpleMarker -> builder.addMarker(MarkerFactory.getMarker(attribute.name))
            }
        }

        builder.setMessage(
            message.processPlaceholders(
                { ARGUMENT_PLACEHOLDER },
                { builder.addArgument(it) },
                { name, value ->
                    builder.addKeyValue(name, value)
                    builder.addArgument(value)
                }
            )
        )

        builder.log()
    }

    private fun applyContextChange(map: Map<String, String?>) {
        for ((key, value) in map) {
            if (value != null) {
                MDC.put(key, value)
            } else {
                MDC.remove(key)
            }
        }
    }

    private inline fun <T> setupLoggingContext(contextChangeMap: Map<String, String?>, block: () -> T): T {
        val previousValues = HashMap<String, String?>(contextChangeMap.size)
        for (key in contextChangeMap.keys) {
            previousValues[key] = MDC.get(key)
        }

        return try {
            applyContextChange(contextChangeMap)
            block()
        } finally {
            applyContextChange(previousValues)
        }
    }

    override fun <T> withNonSuspendableLoggingContext(contextChangeMap: Map<String, String?>, block: () -> T): T =
        setupLoggingContext(contextChangeMap, block)

    override suspend fun <T> withLoggingContext(
        contextChangeMap: Map<String, String?>,
        block: suspend () -> T
    ): T =
        setupLoggingContext(contextChangeMap) {
            withContext(MDCContext()) {
                block()
            }
        }
}
