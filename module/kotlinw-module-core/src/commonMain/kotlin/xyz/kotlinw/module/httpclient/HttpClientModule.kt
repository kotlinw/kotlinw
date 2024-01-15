package xyz.kotlinw.module.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.seconds
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

internal expect fun getPlatformHttpClientEngine(): HttpClientEngineFactory<*>

@Module
class HttpClientModule {

    @Component
    fun defaultHttpClientEngineProvider() = DefaultHttpClientEngineProvider { getPlatformHttpClientEngine() }

    @Component // TODO close!
    fun httpClient(
        defaultHttpClientEngineProvider: DefaultHttpClientEngineProvider,
        customHttpClientEngineProvider: CustomHttpClientEngineProvider?
    ): HttpClient =
        HttpClient(
            customHttpClientEngineProvider?.getCustomHttpClientEngine()
                ?: defaultHttpClientEngineProvider.getDefaultHttpClientEngine()
        ) {
            install(HttpTimeout) {
                connectTimeoutMillis = 3.seconds.inWholeMilliseconds // TODO config
                requestTimeoutMillis = 10.seconds.inWholeMilliseconds // TODO config
            }
        }
}
