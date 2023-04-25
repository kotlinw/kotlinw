package kotlinw.remoting.server.core

import kotlin.jvm.JvmInline

sealed interface RawMessage {

    @JvmInline
    value class Text(val text: String) : RawMessage

    @JvmInline
    value class Binary(val byteArray: ByteArray) : RawMessage
}
