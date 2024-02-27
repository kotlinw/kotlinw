package kotlinw.remoting.server.ktor

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.NewConnectionData
import kotlinw.remoting.core.common.RemovedConnectionData
import kotlinw.uuid.Uuid
import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler

interface RemotingConfiguration {

    val id: String

    val remotingProvider: RemotingProvider

    val messageCodec: MessageCodec<*>?

    val remoteCallHandlers: Collection<RemoteCallHandler<*>>

    val authenticationProviderName: String?

    val extractPrincipal: ApplicationCall.() -> Principal?

    val identifyClient: ApplicationCall.(Principal?) -> MessagingPeerId

    val identifyConnection: ApplicationCall.() -> MessagingConnectionId
}

data class WebRequestRemotingConfiguration(
    override val id: String,
    override val remotingProvider: WebRequestRemotingProvider,
    override val remoteCallHandlers: Collection<RemoteCallHandler<*>>,
    override val authenticationProviderName: String?,
    override val extractPrincipal: ApplicationCall.() -> Principal?,
    override val identifyClient: ApplicationCall.(Principal?) -> MessagingPeerId,
    override val identifyConnection: ApplicationCall.() -> MessagingConnectionId,
    override val messageCodec: MessageCodec<*>? = null
) : RemotingConfiguration

data class WebSocketRemotingConfiguration(
    override val id: String,
    override val remotingProvider: WebSocketRemotingProvider,
    override val remoteCallHandlers: Collection<RemoteCallHandler<*>>,
    override val authenticationProviderName: String?,
    override val extractPrincipal: ApplicationCall.() -> Principal?,
    override val identifyClient: ApplicationCall.(Principal?) -> MessagingPeerId,
    override val identifyConnection: ApplicationCall.() -> MessagingConnectionId = { Uuid.randomUuid() },
    val wsEndpointName: String = id,
    val onConnectionAdded: (suspend (NewConnectionData) -> Unit)? = null,
    val onConnectionRemoved: (suspend (RemovedConnectionData) -> Unit)? = null,
    override val messageCodec: MessageCodec<*>? = null
) : RemotingConfiguration
