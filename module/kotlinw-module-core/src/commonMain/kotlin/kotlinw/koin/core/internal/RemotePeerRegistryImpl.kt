package kotlinw.koin.core.internal

import kotlinw.remoting.core.common.RemoteConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap

class RemotePeerRegistryImpl : MutableRemotePeerRegistry {

    private val logger = PlatformLogging.getLogger()

    private val _connectedPeers: ConcurrentMutableMap<RemoteConnectionId, RemoteConnectionData> =
        ConcurrentHashMap()

    override val connectedPeers: Map<RemoteConnectionId, RemoteConnectionData> get() = _connectedPeers

    override fun addConnection(remoteConnectionId: RemoteConnectionId, remoteConnectionData: RemoteConnectionData) {
        logger.debug { "Adding connection: " / remoteConnectionId }
        _connectedPeers.compute(remoteConnectionId) { _, previousValue ->
            check(previousValue == null)
            remoteConnectionData
        }
    }

    override fun removeConnection(remoteConnectionId: RemoteConnectionId) {
        logger.debug { "Removing connection: " / remoteConnectionId }
        _connectedPeers.remove(remoteConnectionId)
    }
}
