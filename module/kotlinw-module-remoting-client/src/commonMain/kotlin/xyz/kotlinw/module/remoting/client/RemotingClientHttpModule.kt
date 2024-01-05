package xyz.kotlinw.module.remoting.client

import io.ktor.client.HttpClient
import kotlinw.logging.api.LoggerFactory
import kotlinw.remoting.core.codec.MessageCodec
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.httpclient.HttpClientModule

@Module(includeModules = [HttpClientModule::class])
class RemotingClientHttpModule {

    @Component
    fun remotingClientFactory(
        defaultMessageCodec: MessageCodec<*>?,
        httpClient: HttpClient,
        loggerFactory: LoggerFactory
    ): RemotingClientFactory =
        RemotingClientFactoryImpl(defaultMessageCodec, httpClient, loggerFactory)
}
