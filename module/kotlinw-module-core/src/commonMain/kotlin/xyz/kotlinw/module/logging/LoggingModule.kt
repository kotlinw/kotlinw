package xyz.kotlinw.module.logging

import kotlin.concurrent.Volatile
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.platform.PlatformLogging
import kotlinw.logging.spi.LoggingIntegrator
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class LoggingModule {

    companion object {

        @Volatile
        @PublishedApi
        internal var loggingIntegrator: LoggingIntegrator = PlatformLogging

        inline val loggerFactory: LoggerFactory get() = loggingIntegrator
    }

    @Component
    fun loggingIntegrator(loggingIntegratorProvider: LoggingIntegratorProvider?): LoggingIntegrator {
        if (loggingIntegratorProvider != null) {
            loggingIntegrator = loggingIntegratorProvider.getLoggingIntegrator()
        }
        return loggingIntegrator
    }
}
