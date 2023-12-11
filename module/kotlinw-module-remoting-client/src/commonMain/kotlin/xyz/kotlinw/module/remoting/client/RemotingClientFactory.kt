package xyz.kotlinw.module.remoting.client

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.api.internal.server.RemoteCallHandler
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.MutableRemotePeerRegistryImpl
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url

interface RemotingClientFactory {

    fun createRemotingClient(
        remoteServerBaseUrl: Url,
        incomingCallDelegators: Map<String, RemoteCallHandler> = emptyMap(),
        synchronousCallSupportImplementor: SynchronousCallSupport? = null,
        remotePeerRegistry: MutableRemotePeerRegistry = MutableRemotePeerRegistryImpl(),
        messageCodec: MessageCodec<*>? = null,
        httpClientCustomizer: HttpClient.() -> HttpClient = { this },
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {}
    ): RemotingClient
}

class RemotingClientFactoryImpl(
    private val defaultMessageCodec: MessageCodec<*>?,
    private val httpClient: HttpClient
) : RemotingClientFactory {

    override fun createRemotingClient(
        remoteServerBaseUrl: Url,
        incomingCallDelegators: Map<String, RemoteCallHandler>,
        synchronousCallSupportImplementor: SynchronousCallSupport?,
        remotePeerRegistry: MutableRemotePeerRegistry,
        messageCodec: MessageCodec<*>?,
        httpClientCustomizer: HttpClient.() -> HttpClient,
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit
    ): RemotingClient =
        HttpRemotingClient(
            messageCodec
                ?: defaultMessageCodec
                ?: JsonMessageCodec.Default,
            KtorHttpRemotingClientImplementor(httpClient.httpClientCustomizer(), httpRequestCustomizer),
            remotePeerRegistry,
            remoteServerBaseUrl,
            incomingCallDelegators
        )
}
