package kotlinw.logging.spi

import kotlinw.logging.api.Logger

interface LoggerImplementor : Logger {

    val loggingIntegrator: LoggingIntegrator
}
