package kotlinw.logging.platform

import kotlinw.logging.spi.LoggingIntegrator

// TODO this should not be referenced directly from most places
expect object PlatformLogging: LoggingIntegrator
