package xyz.kotlinw.module.logging

import kotlinw.logging.platform.PlatformLogging
import kotlinw.logging.spi.LoggingIntegrator
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class LoggingModule {

    @Component
    fun loggingIntegrator(loggingIntegratorProvider: LoggingIntegratorProvider?): LoggingIntegrator =
        loggingIntegratorProvider?.getLoggingIntegrator() ?: PlatformLogging
}
