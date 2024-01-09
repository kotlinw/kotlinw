package xyz.kotlinw.remoting.api

import kotlinx.coroutines.CoroutineScope

interface PersistentRemotingClient : RemotingClient, CoroutineScope {

    suspend fun connect(): Nothing

    suspend fun close()
}
