package kotlinw.remoting.core.common

data class RemovedConnectionData(
    override val connectionId: RemoteConnectionId,
    override val principal: Any?
) : BasicConnectionData
