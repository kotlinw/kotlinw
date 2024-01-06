package xyz.kotlinw.remoting.api

interface PersistentRemotingClient : RemotingClient {

    suspend fun runMessagingLoop(): Nothing
}
