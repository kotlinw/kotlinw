package kotlinw.logging.platform

import kotlinw.logging.spi.LoggingIntegrator

internal expect val platformLoggingIntegrator: LoggingIntegrator

object PlatformLogging: LoggingIntegrator by platformLoggingIntegrator
