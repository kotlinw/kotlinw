package kotlinw.remoting.server.ktor

import kotlinw.remoting.api.internal.server.RemoteCallHandler
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingSessionId

interface RemotingConfiguration {

    val remotingProvider: RemotingProvider

    val messageCodec: MessageCodec<*>?

    val remoteCallHandlers: Collection<RemoteCallHandler>

    val authenticationProviderName: String?
}

data class WebRequestRemotingConfiguration(
    override val remotingProvider: WebRequestRemotingProvider,
    override val remoteCallHandlers: Collection<RemoteCallHandler>,
    override val authenticationProviderName: String?,
    override val messageCodec: MessageCodec<*>? = null
) : RemotingConfiguration

data class WebSocketRemotingConfiguration(
    override val remotingProvider: WebSocketRemotingProvider,
    override val remoteCallHandlers: Collection<RemoteCallHandler>,
    override val authenticationProviderName: String?,
    private val onConnectionAdded: ((MessagingPeerId, MessagingSessionId, BidirectionalMessagingManager) -> Unit)? = null,
    private val onConnectionRemoved: ((MessagingPeerId, MessagingSessionId) -> Unit)? = null,
    override val messageCodec: MessageCodec<*>? = null
) : RemotingConfiguration
