package kotlinw.logging.platform

import kotlinw.logging.slf4j.Slf4jLoggingIntegrator
import kotlinw.logging.spi.LoggingIntegrator

internal actual val platformLoggingIntegrator: LoggingIntegrator = Slf4jLoggingIntegrator()
