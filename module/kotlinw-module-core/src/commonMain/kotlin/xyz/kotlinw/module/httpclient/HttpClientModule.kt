package xyz.kotlinw.module.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.seconds
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

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
