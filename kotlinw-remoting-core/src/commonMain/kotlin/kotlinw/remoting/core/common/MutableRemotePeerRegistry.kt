package kotlinw.remoting.core.common

import xyz.kotlinw.remoting.api.RemoteConnectionId

interface MutableRemotePeerRegistry : RemotePeerRegistry {

    fun addConnection(remoteConnectionId: RemoteConnectionId, remoteConnectionData: RemoteConnectionData)

    fun removeConnection(remoteConnectionId: RemoteConnectionId)
}
