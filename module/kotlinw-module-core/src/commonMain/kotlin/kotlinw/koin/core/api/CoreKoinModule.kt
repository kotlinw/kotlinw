package kotlinw.koin.core.api

import io.ktor.client.HttpClient
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
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
import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import org.koin.dsl.onClose

fun coreKoinModule() = module {
    single<ContainerStartupCoordinator>(createdAtStart = true) { ContainerStartupCoordinatorImpl() }
    single<ContainerShutdownCoordinator> { ContainerShutdownCoordinatorImpl() } onClose { it?.close() }

    single<LoggingIntegrator> { defaultLoggingIntegrator } withOptions {
        bind<LoggingConfigurationProvider>()
        bind<LoggerFactory>()
        bind<LoggingDelegator>()
        bind<LoggingContextManager>()
    }
    single<ApplicationCoroutineService> { ApplicationCoroutineServiceImpl() }
    single<ConfigurationPropertyLookup> { ConfigurationPropertyLookupImpl(getAll()) }
    single { LocalEventBusImpl(get()) }

    // TODO az alábbiakat külön modulba
    single { HttpClient() }
    single { KtorHttpRemotingClientImplementor(get<HttpClient>()) } withOptions {
        bind<HttpRemotingClient.SynchronousCallSupportImplementor>()
        bind<HttpRemotingClient.BidirectionalCommunicationImplementor>()
    }
    single<RemotingClientManager> { RemotingClientManagerImpl(get()) }
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
