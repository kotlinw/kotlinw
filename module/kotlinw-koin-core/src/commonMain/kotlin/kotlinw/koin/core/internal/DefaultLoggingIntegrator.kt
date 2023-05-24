package kotlinw.koin.core.internal

import kotlinw.logging.platform.PlatformLogging
import kotlinw.logging.spi.LoggingIntegrator

val defaultLoggingIntegrator: LoggingIntegrator = PlatformLogging
