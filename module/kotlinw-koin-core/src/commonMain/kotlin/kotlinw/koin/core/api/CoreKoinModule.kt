package kotlinw.koin.core.api

import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.spi.LoggingConfigurationManager
import kotlinw.logging.spi.LoggingContextManager
import kotlinw.logging.spi.LoggingDelegator
import kotlinw.module.core.api.ApplicationCoroutineService
import kotlinw.module.core.impl.ApplicationCoroutineServiceImpl
import kotlinw.module.core.impl.defaultLoggingIntegrator
import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.client.HttpRemotingClient.SynchronousCallSupportImplementor
import kotlinw.remoting.core.ktor.GenericTextMessageCodec
import kotlinw.util.stdlib.Url
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

fun koinCoreModule() =
    module {
        single { defaultLoggingIntegrator } withOptions {
            bind<LoggingConfigurationManager>()
            bind<LoggerFactory>()
            bind<LoggingDelegator>()
            bind<LoggingContextManager>()
        }
        single<ApplicationCoroutineService> { ApplicationCoroutineServiceImpl() }
        single { LocalEventBusImpl(get()) }
        single { HttpClient() }
        single<SynchronousCallSupportImplementor> { KtorHttpRemotingClientImplementor(get<HttpClient>()) }
        factory<RemotingClient> { (remoteServerBaseUrl: Url) ->
            HttpRemotingClient(
                GenericTextMessageCodec(Json, ContentType.Application.Json),
                get(),
                remoteServerBaseUrl
            )
        }
    }
