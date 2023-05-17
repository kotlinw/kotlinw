package kotlinw.koin.core.api

import io.ktor.client.HttpClient
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.spi.LoggingConfigurationProvider
import kotlinw.logging.spi.LoggingContextManager
import kotlinw.logging.spi.LoggingDelegator
import kotlinw.module.core.api.ApplicationCoroutineService
import kotlinw.module.core.impl.ApplicationCoroutineServiceImpl
import kotlinw.module.core.impl.defaultLoggingIntegrator
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

fun coreKoinModule() =
    module {
        single { defaultLoggingIntegrator } withOptions {
            bind<LoggingConfigurationProvider>()
            bind<LoggerFactory>()
            bind<LoggingDelegator>()
            bind<LoggingContextManager>()
        }
        single<ApplicationCoroutineService> { ApplicationCoroutineServiceImpl() }
        single<ConfigurationPropertyLookup> { ConfigurationPropertyLookupImpl(getAll()) }
        single { LocalEventBusImpl(get()) }
        single { HttpClient() }
        single { KtorHttpRemotingClientImplementor(get<HttpClient>()) } withOptions {
            bind<HttpRemotingClient.SynchronousCallSupportImplementor>()
            bind<HttpRemotingClient.BidirectionalCommunicationImplementor>()
        }
        single<RemotingClientManager> { RemotingClientManagerImpl(get()) }
    }

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
