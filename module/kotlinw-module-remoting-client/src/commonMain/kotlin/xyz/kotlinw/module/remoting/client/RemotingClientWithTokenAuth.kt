package xyz.kotlinw.module.remoting.client

import io.ktor.client.request.HttpRequestBuilder
import kotlinw.logging.api.Logger
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url
import xyz.kotlinw.module.remoting.client.RemotingClientFactory.Companion.applyDefaultWebSocketRemotingClientConfig
import xyz.kotlinw.oauth2.ktor.client.configureTokenAuth
import xyz.kotlinw.remoting.api.PersistentRemotingClient
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler

fun RemotingClientFactory.createWebSocketRemotingClientWithTokenAuth(
    remoteServerBaseUrl: Url,
    webSocketEndpointId: String,
    incomingCallDelegators: Set<RemoteCallHandler<*>> = emptySet(),
    synchronousCallSupportImplementor: SynchronousCallSupport? = null,
    httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {},
    tokenEndpointUrl: Url,
    clientId: String,
    clientSecret: String,
    logger: Logger
): PersistentRemotingClient {

    return createWebSocketRemotingClient(
        remoteServerBaseUrl,
        webSocketEndpointId,
        incomingCallDelegators,
        synchronousCallSupportImplementor,
        httpRequestCustomizer
    ) {
        it.config {
            applyDefaultWebSocketRemotingClientConfig(it)
            configureTokenAuth(tokenEndpointUrl, clientId, clientSecret, logger)
        }
    }
}
