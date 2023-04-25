package kotlinw.remoting.core

import kotlinw.remoting.server.core.MessageCodec

interface MessageCodecImplementor: MessageCodec {

    val descriptor: MessageCodecDescriptor
}
