package kotlinw.logging.spi

import kotlinw.logging.api.LogLevel

interface LoggingConfigurationManager {

    fun setRootLogLevel(level: LogLevel)

    fun setLoggerLogLevel(logger: LoggerImplementor, level: LogLevel)
}
