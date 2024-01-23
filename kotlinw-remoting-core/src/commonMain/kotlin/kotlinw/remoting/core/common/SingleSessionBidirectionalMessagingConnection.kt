package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import xyz.kotlinw.remoting.api.RemoteCallContext
import xyz.kotlinw.remoting.api.RemoteConnectionId

class MessagingChannelDisconnectedException(): RuntimeException()

interface SingleSessionBidirectionalMessagingConnection : CoroutineScope {

    val remoteConnectionId: RemoteConnectionId

    suspend fun incomingRawMessages(): Flow<RawMessage>

    suspend fun sendRawMessage(rawMessage: RawMessage)

    suspend fun close()
}
