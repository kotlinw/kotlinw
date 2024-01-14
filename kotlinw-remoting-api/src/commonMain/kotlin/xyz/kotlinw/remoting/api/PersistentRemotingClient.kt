package xyz.kotlinw.remoting.api

import kotlinx.coroutines.CoroutineScope
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport
import xyz.kotlinw.remoting.api.internal.RemotingClientFlowSupport

interface PersistentRemotingClient: CoroutineScope {

    val isConnected: Boolean

    suspend fun connectAndRunMessageLoop(): Nothing

    suspend fun close()

    suspend fun withConnection(
        block: suspend (PersistentRemotingConnection) -> Unit
    )
}

interface PersistentRemotingConnection: RemotingClient, RemotingClientCallSupport, RemotingClientFlowSupport {

    val coroutineScope: CoroutineScope

    suspend fun close()
}
