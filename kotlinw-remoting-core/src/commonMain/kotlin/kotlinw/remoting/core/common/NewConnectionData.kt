package kotlinw.remoting.core.common

import kotlinx.coroutines.CoroutineScope
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.RemotingClient

data class NewConnectionData(
    override val remotePeerId: MessagingPeerId,
    override val connectionId: MessagingConnectionId,
    val reverseRemotingClient: RemotingClient,
    val coroutineScope: CoroutineScope
) :
    BasicConnectionData
