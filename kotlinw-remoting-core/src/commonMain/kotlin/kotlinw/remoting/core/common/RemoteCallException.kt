package kotlinw.remoting.core.common

import kotlinw.remoting.core.codec.MessageCodecDescriptor

class RemoteCallException(
    val url: String,
    val rawParameter: Any?,
    val messageCodecDescriptor: MessageCodecDescriptor,
    val responseStatusCode: Int?,
    val response: Any?
) : RuntimeException()
