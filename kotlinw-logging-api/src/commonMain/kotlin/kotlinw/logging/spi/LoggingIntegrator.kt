package kotlinw.logging.spi

import kotlinw.logging.api.LoggerFactory

interface LoggingIntegrator: LoggingConfigurationProvider, LoggerFactory, LoggingDelegator, LoggingContextManager
