package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface BidirectionalRawMessagingConnection : CoroutineScope {

    suspend fun incomingRawMessages(): Flow<RawMessage>

    suspend fun sendRawMessage(rawMessage: RawMessage)
}
