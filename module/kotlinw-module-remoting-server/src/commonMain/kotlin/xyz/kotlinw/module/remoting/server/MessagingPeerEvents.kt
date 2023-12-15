package xyz.kotlinw.module.remoting.server

import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingConnectionId

data class MessagingPeerConnectedEvent(
    val peerId: MessagingPeerId,
    val sessionId: MessagingConnectionId
)

data class MessagingPeerDisconnectedEvent(
    val peerId: MessagingPeerId,
    val sessionId: MessagingConnectionId
)
