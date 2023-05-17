package kotlinw.logging.logback

import ch.qos.logback.classic.LoggerContext
import kotlinw.logging.api.LogLevel
import kotlinw.logging.slf4j.Slf4jLoggingIntegrator
import kotlinw.logging.spi.LoggerImplementor
import kotlinw.logging.spi.LoggingConfigurationManager
import kotlinw.logging.spi.LoggingIntegrator
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level as LogbackLevel
import ch.qos.logback.classic.Logger as LogbackLogger
import org.slf4j.Logger as Slf4jLogger

class LogbackLoggingIntegrator private constructor(
    slf4jLoggingIntegrator: Slf4jLoggingIntegrator
) : LoggingIntegrator by slf4jLoggingIntegrator, LoggingConfigurationManager {

    constructor() : this(Slf4jLoggingIntegrator())

    init {
        check(LoggerFactory.getILoggerFactory() is LoggerContext)
    }

    override fun setRootLogLevel(level: LogLevel) {
        setLoggerLogLevel(getLogger(Slf4jLogger.ROOT_LOGGER_NAME) as LoggerImplementor, level)
    }

    private fun LogLevel.asLogbackLevel(): LogbackLevel =
        when (this) {
            LogLevel.Error -> LogbackLevel.ERROR
            LogLevel.Warning -> LogbackLevel.WARN
            LogLevel.Info -> LogbackLevel.INFO
            LogLevel.Debug -> LogbackLevel.DEBUG
            LogLevel.Trace -> LogbackLevel.TRACE
        }

    override fun setLoggerLogLevel(logger: LoggerImplementor, level: LogLevel) {
        val nativeLogger = (logger as Slf4jLoggingIntegrator.Slf4jLoggerWrapper).slf4jLogger
        if (nativeLogger is LogbackLogger) {
            nativeLogger.level = level.asLogbackLevel()
        }
    }
}
