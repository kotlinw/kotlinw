package kotlinw.remoting.server.core

import kotlin.jvm.JvmInline

sealed interface RawMessage {

    fun toByteArray(): ByteArray

    @JvmInline
    value class Text(val text: String) : RawMessage {

        companion object {

            fun of(bytes: ByteArray) = Text(bytes.decodeToString())
        }

        override fun toByteArray() = text.encodeToByteArray()
    }

    @JvmInline
    value class Binary(val byteArray: ByteArray) : RawMessage {

        override fun toByteArray() = byteArray
    }
}
