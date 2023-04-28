package kotlinw.logging.spi

import kotlinw.logging.api.LogLevel
import kotlinw.logging.api.Logger

interface LoggingConfigurationManager {

    fun isLogLevelEnabled(logger: LoggerImplementor, level: LogLevel): Boolean
}
