package kotlinw.remoting.core.common

import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import xyz.kotlinw.remoting.api.RemoteConnectionId

class MutableRemotePeerRegistryImpl(
    loggerFactory: LoggerFactory
) : MutableRemotePeerRegistry {

    private val logger = loggerFactory.getLogger()

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
