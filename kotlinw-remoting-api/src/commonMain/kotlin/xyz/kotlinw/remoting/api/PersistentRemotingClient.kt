package xyz.kotlinw.remoting.api

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport
import xyz.kotlinw.remoting.api.internal.RemotingClientFlowSupport

interface PersistentRemotingClient {

    val isConnectedStateFlow: Flow<Boolean>

    val isConnected: Boolean

    /**
     * Connects to the remote server and runs the message loop processing incoming messages.
     *
     * @param communicationCircuitBreaker if specified, then the given flow should emit `false` to pause the message loop, and `true` to resume it again
     */
    suspend fun connectAndRunMessageLoop(
        onConnected: () -> Unit = {},
        handleException: suspend (Throwable) -> Unit = {}, // TODO rename to onDisconnected ?
        beforeAutomaticReconnect: suspend () -> Unit = { delay(5.seconds) },
        communicationCircuitBreaker: Flow<Boolean>? = null
    ): Nothing

    /**
     * Runs the given `block` of code in the context of a remote connection in a separate coroutine.
     * If the server is not connected yet (or [connectAndRunMessageLoop] has not been called yet) then suspends the current coroutine until the connection is established.
     * If the server is disconnected before [block] is completed then the coroutine running [block] is cancelled but the current coroutine running [withConnection] is not cancelled, instead it returns with a failure.
     */
    suspend fun <T> withConnection(block: suspend (PersistentRemotingConnection) -> T): Result<T>
}

interface PersistentRemotingConnection :
    RemotingClient, RemotingClientCallSupport, RemotingClientFlowSupport, CoroutineScope {

    suspend fun close()
}
