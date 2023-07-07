package kotlinw.module.serverbase

import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingSessionId

data class MessagingPeerConnectedEvent(
    val peerId: MessagingPeerId,
    val sessionId: MessagingSessionId
)

data class MessagingPeerDisconnectedEvent(
    val peerId: MessagingPeerId,
    val sessionId: MessagingSessionId
)
