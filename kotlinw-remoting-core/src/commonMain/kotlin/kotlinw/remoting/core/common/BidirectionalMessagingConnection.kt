package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingConnectionId

class MessagingChannelDisconnectedException(): RuntimeException()

interface BidirectionalMessagingConnection : CoroutineScope {

    val peerId: MessagingPeerId

    val sessionId: MessagingConnectionId

    suspend fun incomingRawMessages(): Flow<RawMessage>

    suspend fun sendRawMessage(rawMessage: RawMessage)

    suspend fun close()
}
