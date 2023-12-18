package xyz.kotlinw.module.logging

import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.platform.PlatformLogging
import kotlinw.logging.spi.LoggingIntegrator
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

private val loggingIntegratorHolder: AtomicRef<LoggingIntegrator> = atomic(PlatformLogging)

val loggerFactory: LoggerFactory by loggingIntegratorHolder

internal fun setLoggingIntegrator(loggingIntegrator: LoggingIntegrator) {
    loggingIntegratorHolder.value = loggingIntegrator
}

@Module
class LoggingModule {

    @Component
    fun loggingIntegrator(loggingIntegratorProvider: LoggingIntegratorProvider?): LoggingIntegrator {
        if (loggingIntegratorProvider != null) {
            setLoggingIntegrator(loggingIntegratorProvider.getLoggingIntegrator())
        }
        return loggingIntegratorHolder.value
    }
}
