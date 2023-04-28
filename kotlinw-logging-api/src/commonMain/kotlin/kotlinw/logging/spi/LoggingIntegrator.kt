package kotlinw.logging.spi

import kotlinw.logging.api.LoggerFactory

interface LoggingIntegrator: LoggingConfigurationManager, LoggerFactory, LoggingDelegator, LoggingContextManager
