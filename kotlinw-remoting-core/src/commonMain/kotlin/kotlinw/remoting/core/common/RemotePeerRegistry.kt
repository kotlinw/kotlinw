package kotlinw.remoting.core.common

import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingSessionId

data class RemoteConnectionId(val messagingPeerId: MessagingPeerId, val messagingSessionId: MessagingSessionId)

data class RemoteConnectionData(val messagingManager: BidirectionalMessagingManager)

interface RemotePeerRegistry {

    val connectedPeers: Map<RemoteConnectionId, RemoteConnectionData>
}
