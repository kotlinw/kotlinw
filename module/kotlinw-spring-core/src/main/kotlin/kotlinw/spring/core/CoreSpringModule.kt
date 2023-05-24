package kotlinw.spring.core

import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.koin.core.internal.ApplicationCoroutineServiceImpl
import kotlinw.koin.core.internal.defaultLoggingIntegrator
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.spi.LoggingIntegrator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class CoreSpringModule {

    @Bean
    fun loggingIntegrator(): LoggingIntegrator = defaultLoggingIntegrator

    @Bean
    fun applicationCoroutineService(): ApplicationCoroutineService = ApplicationCoroutineServiceImpl()

    @Bean
    fun localEventBus(loggerFactory: LoggerFactory): LocalEventBus = LocalEventBusImpl(1000) // TODO config
}
