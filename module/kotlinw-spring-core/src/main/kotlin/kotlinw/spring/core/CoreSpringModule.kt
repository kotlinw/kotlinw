package kotlinw.spring.core

import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.module.core.api.ApplicationCoroutineService
import kotlinw.module.core.impl.ApplicationCoroutineServiceImpl
import kotlinw.module.core.impl.defaultLoggingIntegrator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class CoreSpringModule {

    @Bean
    fun loggingIntegrator(): LoggingIntegrator = defaultLoggingIntegrator

    @Bean
    fun applicationCoroutineService(): ApplicationCoroutineService = ApplicationCoroutineServiceImpl()

    @Bean
    fun localEventBus(loggerFactory: LoggerFactory): LocalEventBus = LocalEventBusImpl(loggerFactory)
}
