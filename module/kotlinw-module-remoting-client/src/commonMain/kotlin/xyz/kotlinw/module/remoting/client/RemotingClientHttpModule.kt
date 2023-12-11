package xyz.kotlinw.module.remoting.client

import io.ktor.client.HttpClient
import kotlinw.remoting.core.codec.MessageCodec
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.koin.core.api.HttpClientModule

@Module(includeModules = [HttpClientModule::class])
class RemotingClientHttpModule {

    @Component
    fun remotingClientFactory(defaultMessageCodec: MessageCodec<*>?, httpClient: HttpClient): RemotingClientFactory =
        RemotingClientFactoryImpl(defaultMessageCodec, httpClient)
}
