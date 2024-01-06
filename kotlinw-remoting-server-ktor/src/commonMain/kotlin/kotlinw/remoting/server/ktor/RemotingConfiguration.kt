package kotlinw.remoting.server.ktor

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.NewConnectionData
import kotlinw.remoting.core.common.RemovedConnectionData
import xyz.kotlinw.remoting.api.MessagingPeerId

interface RemotingConfiguration {

    val id: String

    val remotingProvider: RemotingProvider

    val messageCodec: MessageCodec<*>?

    val remoteCallHandlers: Collection<RemoteCallHandler<*>>

    val authenticationProviderName: String?

    val identifyClient: (ApplicationCall) -> MessagingPeerId

    val extractPrincipal: (ApplicationCall) -> Principal?
}

data class WebRequestRemotingConfiguration(
    override val id: String,
    override val remotingProvider: WebRequestRemotingProvider,
    override val remoteCallHandlers: Collection<RemoteCallHandler<*>>,
    override val authenticationProviderName: String?,
    override val identifyClient: (ApplicationCall) -> MessagingPeerId,
    override val extractPrincipal: (ApplicationCall) -> Principal?,
    override val messageCodec: MessageCodec<*>? = null
) : RemotingConfiguration

data class WebSocketRemotingConfiguration(
    override val id: String,
    override val remotingProvider: WebSocketRemotingProvider,
    override val remoteCallHandlers: Collection<RemoteCallHandler<*>>,
    override val authenticationProviderName: String?,
    override val identifyClient: (ApplicationCall) -> MessagingPeerId,
    override val extractPrincipal: (ApplicationCall) -> Principal?,
    val onConnectionAdded: ((NewConnectionData) -> Unit)? = null,
    val onConnectionRemoved: ((RemovedConnectionData) -> Unit)? = null,
    override val messageCodec: MessageCodec<*>? = null
) : RemotingConfiguration
