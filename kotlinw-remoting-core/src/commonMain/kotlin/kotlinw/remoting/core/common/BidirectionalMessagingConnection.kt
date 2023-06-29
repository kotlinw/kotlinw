package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface BidirectionalMessagingConnection<M : RawMessage> : CoroutineScope {

    suspend fun incomingRawMessages(): Flow<M>

    suspend fun sendMessage(rawMessage: M)
}
