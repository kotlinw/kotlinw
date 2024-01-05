package kotlinw.remoting.core.client

import xyz.kotlinw.remoting.api.RemotingClient

interface PersistentRemotingClient: RemotingClient {

    suspend fun runMessagingLoop(): Nothing
}
