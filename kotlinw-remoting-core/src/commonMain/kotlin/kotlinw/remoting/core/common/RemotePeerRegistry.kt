package kotlinw.remoting.core.common

import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.remoting.api.RemotingClient

data class RemoteConnectionData(
    val remoteConnectionId: RemoteConnectionId,
    val remotingClient: RemotingClient
    // TODO coroutineScope
)

interface RemotePeerRegistry {

    val connectedPeers: Map<RemoteConnectionId, RemoteConnectionData>
}
