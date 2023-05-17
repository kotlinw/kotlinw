package kotlinw.logging.spi

import kotlinw.logging.api.LogLevel

interface LoggingConfigurationProvider {

    fun isLogLevelEnabled(logger: LoggerImplementor, level: LogLevel): Boolean
}
