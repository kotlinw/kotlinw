package kotlinw.remoting.core.common

import kotlinw.remoting.core.common.RemoteConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.remoting.core.common.RemotePeerRegistry

interface MutableRemotePeerRegistry : RemotePeerRegistry {

    fun addConnection(remoteConnectionId: RemoteConnectionId, remoteConnectionData: RemoteConnectionData)

    fun removeConnection(remoteConnectionId: RemoteConnectionId)
}
