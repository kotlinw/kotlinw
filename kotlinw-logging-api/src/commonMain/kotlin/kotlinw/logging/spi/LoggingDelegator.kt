package kotlinw.logging.spi

import kotlinw.logging.api.LogEntryAttribute
import kotlinw.logging.api.LogLevel
import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.Logger

interface LoggingDelegator {

    fun log(
        logger: LoggerImplementor,
        level: LogLevel,
        cause: Throwable?,
        message: LogMessage,
        attributes: Collection<LogEntryAttribute>
    )
}
