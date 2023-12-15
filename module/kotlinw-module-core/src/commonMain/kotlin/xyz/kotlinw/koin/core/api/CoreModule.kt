package xyz.kotlinw.koin.core.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.seconds
import kotlinw.configuration.core.ConfigurationObjectLookup
import kotlinw.configuration.core.ConfigurationObjectLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.eventbus.local.InProcessEventBus
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.koin.core.internal.ApplicationCoroutineServiceImpl
import kotlinw.koin.core.internal.defaultLoggingIntegrator
import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.SerializerServiceImpl
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ContainerLifecycleListener
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl

@Module
class HttpClientModule {

    @Component // TODO close!
    fun httpClient(): HttpClient =
        HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 3.seconds.inWholeMilliseconds // TODO config
                requestTimeoutMillis = 10.seconds.inWholeMilliseconds // TODO config
            }
        }
}

@Module
class SerializerModule {

    @Component
    fun serializerService(): SerializerService = SerializerServiceImpl()
}

@Module
class ConfigurationModule {

    @Component
    suspend fun configurationPropertyLookup(configurationPropertyLookupSources: List<ConfigurationPropertyLookupSource>): ConfigurationPropertyLookup =
        ConfigurationPropertyLookupImpl(configurationPropertyLookupSources).also { it.initialize() }

    @Component
    fun configurationObjectLookup(configurationPropertyLookup: ConfigurationPropertyLookup): ConfigurationObjectLookup =
        ConfigurationObjectLookupImpl(configurationPropertyLookup)
}

@Module
class LoggingModule {

    @Component
    fun loggingIntegrator(): LoggingIntegrator = defaultLoggingIntegrator
}

@Module(includeModules = [SerializerModule::class, LoggingModule::class, ConfigurationModule::class])
class CoreModule {

    @Component(type = ContainerLifecycleCoordinator::class)
    fun containerLifecycleCoordinator(listeners: List<ContainerLifecycleListener>) =
        ContainerLifecycleCoordinatorImpl(listeners)

    @Component(type = ApplicationCoroutineService::class, onTerminate = "close")
    fun applicationCoroutineService() = ApplicationCoroutineServiceImpl()

    @Component
    fun localEventBus(): InProcessEventBus = InProcessEventBus()
}
