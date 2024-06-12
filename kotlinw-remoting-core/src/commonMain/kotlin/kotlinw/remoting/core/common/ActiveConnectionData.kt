package kotlinw.remoting.core.common

import kotlinx.coroutines.CoroutineScope
import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.remoting.api.RemotingClient

data class ActiveConnectionData(

    override val connectionId: RemoteConnectionId,

    val reverseRemotingClient: RemotingClient,

    /**
     * Should not be explicitly cancelled. Call `close` which implicitly cancels the `coroutineScope`.
     */
    val coroutineScope: CoroutineScope,

    val close: suspend () -> Unit

) : BasicConnectionData
