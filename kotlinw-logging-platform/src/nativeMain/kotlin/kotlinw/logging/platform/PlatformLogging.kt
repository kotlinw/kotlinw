package kotlinw.logging.platform

import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.logging.stdout.StdoutLoggingIntegrator

internal actual val platformLoggingIntegrator: LoggingIntegrator = StdoutLoggingIntegrator
