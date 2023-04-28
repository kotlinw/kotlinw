package kotlinw.logging.platform

import kotlinw.logging.js.console.ConsoleLoggingIntegrator
import kotlinw.logging.spi.LoggingIntegrator

internal actual val platformLoggingIntegrator: LoggingIntegrator = ConsoleLoggingIntegrator()
