package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecDescriptor

interface SynchronousCallSupport {

    /**
     * @throws [RemoteCallException] if the remote call fails for technical reasons (for example if the server is not available, or it responds with 401 HTTP status)
     */
    suspend fun <M : RawMessage> call(
        url: String /* TODO String helyett Url */,
        rawParameter: M,
        messageCodecDescriptor: MessageCodecDescriptor
    ): M
}
