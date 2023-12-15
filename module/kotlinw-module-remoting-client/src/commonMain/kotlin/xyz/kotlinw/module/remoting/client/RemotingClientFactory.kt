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
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {},
        httpClientCustomizer: HttpClient.() -> HttpClient = { this }
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
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit,
        httpClientCustomizer: HttpClient.() -> HttpClient
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

// TODO valahogy külön kell választani a remoting-ban azt, hogy kétirányú-e a kapcsolat; simán a kliens dönthesse el,
//  hogy ő hogyan kapcsolódik; a lényeg, hogy akkor is tudjon a kliens ws-en kapcsolódni,
//  ha a szerver nem ajánl ki egyetlen remote service-t sem
// TODO esetleg maga a kliens küldhetné fel, hogy milyen RPC megoldásokat támogat, pl. ajánlott-e ki RPC szolgáltatásokat
