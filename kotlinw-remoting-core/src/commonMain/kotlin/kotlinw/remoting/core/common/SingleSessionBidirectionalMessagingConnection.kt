package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class MessagingChannelDisconnectedException(): RuntimeException()

interface SingleSessionBidirectionalMessagingConnection : CoroutineScope {

    val remoteConnectionId: RemoteConnectionId

    suspend fun incomingRawMessages(): Flow<RawMessage>

    suspend fun sendRawMessage(rawMessage: RawMessage)

    suspend fun close()
}
