package kotlinw.logging.platform

import kotlinw.logging.slf4j.Slf4jLoggingIntegrator
import kotlinw.logging.spi.LoggingIntegrator

actual object PlatformLogging : LoggingIntegrator by Slf4jLoggingIntegrator()
