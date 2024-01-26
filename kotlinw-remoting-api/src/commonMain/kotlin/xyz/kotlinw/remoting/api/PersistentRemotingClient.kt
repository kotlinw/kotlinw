package xyz.kotlinw.remoting.api

import kotlinx.coroutines.CoroutineScope
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport
import xyz.kotlinw.remoting.api.internal.RemotingClientFlowSupport

interface PersistentRemotingClient {

    val isConnected: Boolean

    /**
     * Connects to the remote server and runs the message loop processing incoming messages.
     */
    suspend fun connectAndRunMessageLoop(): Nothing

    /**
     * Runs the given `block` of code in the context of a remote connection.
     * If the server is not connected yet (or [connectAndRunMessageLoop] has not been called yet) then suspends the current until the connection is established.
     * If the server is disconnected before [block] is completed then the coroutine running [block] is cancelled but [withConnection] returns with a failure (it is not cancelled).
     */
    suspend fun <T> withConnection(block: suspend (PersistentRemotingConnection) -> T): Result<T>
}

interface PersistentRemotingConnection : RemotingClient, RemotingClientCallSupport, RemotingClientFlowSupport,
    CoroutineScope {

    suspend fun close()
}
