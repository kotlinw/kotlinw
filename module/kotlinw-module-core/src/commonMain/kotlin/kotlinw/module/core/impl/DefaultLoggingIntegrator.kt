package kotlinw.module.core.impl

import kotlinw.logging.platform.PlatformLogging
import kotlinw.logging.spi.LoggingIntegrator

val defaultLoggingIntegrator: LoggingIntegrator = PlatformLogging
