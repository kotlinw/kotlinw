package kotlinw.koin.core.api

import io.ktor.client.HttpClient
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.koin.core.internal.ApplicationCoroutineServiceImpl
import kotlinw.koin.core.internal.ContainerShutdownCoordinator
import kotlinw.koin.core.internal.ContainerShutdownCoordinatorImpl
import kotlinw.koin.core.internal.ContainerStartupCoordinator
import kotlinw.koin.core.internal.ContainerStartupCoordinatorImpl
import kotlinw.koin.core.internal.defaultLoggingIntegrator
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.spi.LoggingConfigurationProvider
import kotlinw.logging.spi.LoggingContextManager
import kotlinw.logging.spi.LoggingDelegator
import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.module.api.ApplicationInitializerService
import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.SerializerServiceImpl
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

val coreModule by lazy {
    module {
        single<ContainerStartupCoordinator> { ContainerStartupCoordinatorImpl() }
        single<ApplicationInitializerService>(named("ContainerStartupCoordinatorApplicationInitializerService")) {
            ApplicationInitializerService(Priority.Normal.lowerBy(100)) {
                get<ContainerStartupCoordinator>().runStartupTasks()
            }
        }

        single<ContainerShutdownCoordinator> { ContainerShutdownCoordinatorImpl() }.onClose { it?.close() }

        single<LoggingIntegrator> { defaultLoggingIntegrator } withOptions {
            bind<LoggingConfigurationProvider>()
            bind<LoggerFactory>()
            bind<LoggingDelegator>()
            bind<LoggingContextManager>()
        }
        single<ApplicationCoroutineService>(createdAtStart = true) {
            ApplicationCoroutineServiceImpl()
                .registerShutdownTask(this) {
                    it.close()
                }
        }

        single<ConfigurationPropertyLookup> { ConfigurationPropertyLookupImpl(getAll()) }

        single<LocalEventBus> {
            LocalEventBusImpl(1000) // TODO config
        }

        single<SerializerService> { SerializerServiceImpl() }

        // TODO az alábbiakat külön modulba
        single { HttpClient() }
        single { KtorHttpRemotingClientImplementor(get<HttpClient>()) } withOptions {
            bind<HttpRemotingClient.SynchronousCallSupportImplementor>()
            bind<HttpRemotingClient.BidirectionalCommunicationImplementor>()
        }
        single { RemotingClientManagerImpl(get()) }.bind<RemotingClientManager>()
    }
}

// TODO move to remoting-client module
interface RemotingClientManager {

    operator fun get(remoteServerBaseUrl: Url): RemotingClient
}

internal class RemotingClientManagerImpl(
    private val synchronousCallSupportImplementor: HttpRemotingClient.SynchronousCallSupportImplementor
) : RemotingClientManager {

    private val remotingClientCache: ConcurrentMutableMap<Url, RemotingClient> = ConcurrentHashMap()

    override fun get(remoteServerBaseUrl: Url): RemotingClient =
        remotingClientCache.computeIfAbsent(remoteServerBaseUrl) {
            HttpRemotingClient(
                JsonMessageCodec.Default, // TODO configurable
                synchronousCallSupportImplementor,
                remoteServerBaseUrl
            )
        }!!
}
