package xyz.kotlinw.remoting.api

data class MessagingPeerConnectedEvent(
    val peerId: MessagingPeerId,
    val sessionId: MessagingSessionId
)

data class MessagingPeerDisconnectedEvent(
    val peerId: MessagingPeerId,
    val sessionId: MessagingSessionId
)
