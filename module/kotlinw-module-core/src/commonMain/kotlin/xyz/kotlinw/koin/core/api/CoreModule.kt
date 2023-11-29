package xyz.kotlinw.koin.core.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.seconds
import kotlinw.configuration.core.ConfigurationObjectLookup
import kotlinw.configuration.core.ConfigurationObjectLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.koin.core.api.ContainerLifecycleListener
import kotlinw.koin.core.api.RemotingClientFactory
import kotlinw.koin.core.api.RemotingClientFactoryImpl
import kotlinw.koin.core.internal.ApplicationCoroutineServiceImpl
import kotlinw.koin.core.internal.defaultLoggingIntegrator
import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.SerializerServiceImpl
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.koin.core.api.internal.ContainerLifecycleCoordinator
import xyz.kotlinw.koin.core.api.internal.ContainerLifecycleCoordinatorImpl

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

@Module(includeModules = [HttpClientModule::class])
class HttpRemotingClientModule {

    @Component
    fun remotingClientImplementor(httpClient: HttpClient) = KtorHttpRemotingClientImplementor(httpClient)

    @Component
    fun remotingClientFactory(defaultSynchronousCallSupportImplementor: SynchronousCallSupport?): RemotingClientFactory =
        RemotingClientFactoryImpl(JsonMessageCodec.Default, defaultSynchronousCallSupportImplementor)
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

@Module(includeModules = [SerializerModule::class, HttpClientModule::class, HttpRemotingClientModule::class, LoggingModule::class, ConfigurationModule::class])
class CoreModule {

    @Component
    fun containerLifecycleCoordinator(listeners: List<ContainerLifecycleListener>): ContainerLifecycleCoordinator =
        ContainerLifecycleCoordinatorImpl(listeners)

    @Component(type = ApplicationCoroutineService::class, onTerminate = "close")
    fun applicationCoroutineService() = ApplicationCoroutineServiceImpl()

    @Component
    fun localEventBus(): LocalEventBus = LocalEventBusImpl()
}
