package xyz.kotlinw.remoting.api

import kotlinx.coroutines.CoroutineScope

interface PersistentRemotingClient : RemotingClient, CoroutineScope {

    suspend fun runMessagingLoop(): Nothing

    suspend fun close()
}
