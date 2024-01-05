package kotlinw.remoting.core.common

data class RemovedConnectionData(
    override val connectionId: RemoteConnectionId
) : BasicConnectionData
