package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecDescriptor

interface SynchronousCallSupport {

    suspend fun <M : RawMessage> call(url: String, rawParameter: M, messageCodecDescriptor: MessageCodecDescriptor): M
}
