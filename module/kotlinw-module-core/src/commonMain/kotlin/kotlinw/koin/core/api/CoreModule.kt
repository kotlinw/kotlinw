package kotlinw.koin.core.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinw.configuration.core.ConfigurationObjectLookup
import kotlinw.configuration.core.ConfigurationObjectLookupImpl
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
import kotlin.time.Duration.Companion.seconds
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.RemotePeerRegistryImpl
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.BidirectionalCommunicationImplementor
import kotlinw.remoting.core.common.RemotePeerRegistry
import kotlinw.remoting.core.common.SynchronousCallSupport

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

        single<ConfigurationPropertyLookup>(createdAtStart = true) {
            ConfigurationPropertyLookupImpl(getAll())
                .also {
                    get<ApplicationCoroutineService>().runBlocking {
                        it.initialize()
                    }
                }
        }

        single<ConfigurationObjectLookup> { ConfigurationObjectLookupImpl(get()) }

        single<LocalEventBus> {
            LocalEventBusImpl(1000) // TODO config
        }

        single<SerializerService> { SerializerServiceImpl() }

        // TODO az alábbiakat külön modulba
        single {
            HttpClient {
                install(HttpTimeout) {
                    connectTimeoutMillis = 3.seconds.inWholeMilliseconds // TODO config
                    requestTimeoutMillis = 10.seconds.inWholeMilliseconds // TODO config
                }
            }
        } // TODO close()-zal le kell zárni
        single { KtorHttpRemotingClientImplementor(get<HttpClient>()) } withOptions {
            bind<SynchronousCallSupport>()
            bind<BidirectionalCommunicationImplementor>()
        }
        single { RemotingClientManagerImpl(get()) }.bind<RemotingClientManager>()
        single { RemotePeerRegistryImpl() }.withOptions {
            bind<RemotePeerRegistry>()
            bind<MutableRemotePeerRegistry>()
        }
    }
}

// TODO remove
interface RemotingClientManager {

    operator fun get(remoteServerBaseUrl: Url, messageCodec: MessageCodec<*>): RemotingClient
}

internal class RemotingClientManagerImpl(
    private val synchronousCallSupportImplementor: SynchronousCallSupport
) : RemotingClientManager {

    private val remotingClients: ConcurrentMutableMap<Url, RemotingClient> = ConcurrentHashMap()

    override fun get(remoteServerBaseUrl: Url, messageCodec: MessageCodec<*>): RemotingClient =
        remotingClients.computeIfAbsent(remoteServerBaseUrl) {
            HttpRemotingClient(
                messageCodec,
                synchronousCallSupportImplementor,
                RemotePeerRegistryImpl(),
                remoteServerBaseUrl,
                emptyMap()
            )
        }!!
}
