package kotlinw.remoting.core.common

import kotlinx.coroutines.CoroutineScope
import xyz.kotlinw.remoting.api.RemoteCallContext
import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.remoting.api.RemotingClient

data class NewConnectionData(
    override val connectionId: RemoteConnectionId,
    val reverseRemotingClient: RemotingClient,
    val coroutineScope: CoroutineScope
) : BasicConnectionData
