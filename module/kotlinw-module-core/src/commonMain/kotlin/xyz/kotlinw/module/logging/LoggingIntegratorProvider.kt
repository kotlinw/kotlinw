package xyz.kotlinw.module.logging

import kotlinw.logging.spi.LoggingIntegrator

interface LoggingIntegratorProvider {

    fun getLoggingIntegrator(): LoggingIntegrator
}
