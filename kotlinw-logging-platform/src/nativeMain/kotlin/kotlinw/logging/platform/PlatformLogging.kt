package kotlinw.logging.platform

import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.logging.stdout.StdoutLoggingIntegrator

actual object PlatformLogging : LoggingIntegrator by StdoutLoggingIntegrator
