package kotlinw.remoting.core.common

import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.util.stdlib.Url

interface BidirectionalCommunicationImplementor {

    suspend fun runInSession(
        url: Url,
        messageCodecDescriptor: MessageCodecDescriptor,
        block: suspend SingleSessionBidirectionalMessagingConnection.() -> Unit
    )
}
