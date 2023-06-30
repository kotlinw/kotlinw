package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

typealias MessagingPeerId = Any

typealias MessagingSessionId = Any

class MessagingChannelDisconnectedException(): RuntimeException()

interface BidirectionalMessagingConnection : CoroutineScope {

    val peerId: MessagingPeerId

    val sessionId: MessagingSessionId

    suspend fun incomingRawMessages(): Flow<RawMessage>

    suspend fun sendRawMessage(rawMessage: RawMessage)
}
