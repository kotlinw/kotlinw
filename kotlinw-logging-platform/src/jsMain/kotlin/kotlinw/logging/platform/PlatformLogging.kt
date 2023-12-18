package kotlinw.logging.platform

import kotlinw.logging.js.console.ConsoleLoggingIntegrator
import kotlinw.logging.spi.LoggingIntegrator

actual object PlatformLogging : LoggingIntegrator by ConsoleLoggingIntegrator
