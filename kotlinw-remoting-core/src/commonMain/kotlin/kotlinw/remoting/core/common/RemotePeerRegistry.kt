package kotlinw.remoting.core.common

import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.MessagingPeerId

data class RemoteConnectionId(
    override val remotePeerId: MessagingPeerId,
    override val connectionId: MessagingConnectionId
) : BasicConnectionData

data class RemoteConnectionData(val messagingManager: BidirectionalMessagingManager)

interface RemotePeerRegistry {

    val connectedPeers: Map<RemoteConnectionId, RemoteConnectionData>
}
