package xyz.kotlinw.module.remoting.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinw.logging.api.LoggerFactory
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.client.WebRequestRemotingClientImpl
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.MutableRemotePeerRegistryImpl
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url
import xyz.kotlinw.remoting.api.PersistentRemotingClient
import xyz.kotlinw.remoting.api.RemotingClient
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler

interface RemotingClientFactory {

    companion object {

        fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installClientWebSockets() {
            install(WebSockets) {
                // TODO elég a szerveren? pingInterval = 30.seconds.inWholeMilliseconds
            }
        }
    }

    fun createWebRequestRemotingClient(
        remoteServerBaseUrl: Url,
        synchronousCallSupportImplementor: SynchronousCallSupport? = null,
        messageCodec: MessageCodec<*>? = null,
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {},
        httpClientCustomizer: HttpClient.() -> HttpClient = { this }
    ): RemotingClient

    fun createWebSocketRemotingClient(
        remoteServerBaseUrl: Url,
        webSocketEndpointId: String,
        parentCoroutineContext: CoroutineContext,
        incomingCallDelegators: Set<RemoteCallHandler<*>> = emptySet(),
        synchronousCallSupportImplementor: SynchronousCallSupport? = null,
        messageCodec: MessageCodec<*>? = null,
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {},
        httpClientCustomizer: (HttpClient) -> HttpClient = {
            it.config {
                it.pluginOrNull(WebSockets) ?: installClientWebSockets()
            }
        }
    ): PersistentRemotingClient
}

class RemotingClientFactoryImpl(
    private val defaultMessageCodec: MessageCodec<*>?,
    private val httpClient: HttpClient,
    private val loggerFactory: LoggerFactory
) : RemotingClientFactory {

    override fun createWebRequestRemotingClient(
        remoteServerBaseUrl: Url,
        synchronousCallSupportImplementor: SynchronousCallSupport?,
        messageCodec: MessageCodec<*>?,
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit,
        httpClientCustomizer: HttpClient.() -> HttpClient
    ): RemotingClient =
        WebRequestRemotingClientImpl(
            messageCodec
                ?: defaultMessageCodec
                ?: JsonMessageCodec.Default,
            KtorHttpRemotingClientImplementor(httpClient.httpClientCustomizer(), loggerFactory, httpRequestCustomizer),
            remoteServerBaseUrl,
            loggerFactory
        )


    override fun createWebSocketRemotingClient(
        remoteServerBaseUrl: Url,
        webSocketEndpointId: String,
        parentCoroutineContext: CoroutineContext,
        incomingCallDelegators: Set<RemoteCallHandler<*>>,
        synchronousCallSupportImplementor: SynchronousCallSupport?,
        messageCodec: MessageCodec<*>?,
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit,
        httpClientCustomizer: HttpClient.() -> HttpClient
    ): PersistentRemotingClient =
        WebSocketRemotingClientImpl(
            messageCodec
                ?: defaultMessageCodec
                ?: JsonMessageCodec.Default,
            KtorHttpRemotingClientImplementor(httpClient.httpClientCustomizer(), loggerFactory, httpRequestCustomizer),
            MutableRemotePeerRegistryImpl(loggerFactory),
            remoteServerBaseUrl,
            webSocketEndpointId,
            incomingCallDelegators,
            loggerFactory,
            parentCoroutineContext
        )
}
