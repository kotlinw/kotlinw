package kotlinw.remoting.core.common

import xyz.kotlinw.remoting.api.RemoteConnectionId

data class RemovedConnectionData(
    override val connectionId: RemoteConnectionId
) : BasicConnectionData
