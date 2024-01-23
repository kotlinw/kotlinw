package xyz.kotlinw.remoting.api

data class RemoteConnectionId(
    val peerId: MessagingPeerId,
    val connectionId: MessagingConnectionId
)
