package kotlinw.remoting.core.common

import kotlinw.remoting.core.client.PersistentRemotingClient
import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.RemotingClient

data class RemoteConnectionId(
    val remotePeerId: MessagingPeerId,
    val connectionId: MessagingConnectionId
)

data class RemoteConnectionData(
    val remoteConnectionId: RemoteConnectionId,
    val remotingClient: RemotingClient
    // TODO coroutineScope
)

interface RemotePeerRegistry {

    val connectedPeers: Map<RemoteConnectionId, RemoteConnectionData>
}
