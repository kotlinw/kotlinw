package kotlinw.remoting.core.common

import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingConnectionId

data class RemovedConnectionData(
    override val remotePeerId: MessagingPeerId,
    override val connectionId: MessagingConnectionId
): BasicConnectionData
