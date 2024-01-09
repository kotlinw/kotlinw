package xyz.kotlinw.remoting.api

import kotlinx.coroutines.CoroutineScope

interface PersistentRemotingClient : RemotingClient, CoroutineScope {

    val isConnected: Boolean

    suspend fun connect(): Nothing

    suspend fun close()
}
