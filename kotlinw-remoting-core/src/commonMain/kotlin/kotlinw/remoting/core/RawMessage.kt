package kotlinw.remoting.core

import kotlinw.util.stdlib.ByteArrayView
import kotlinw.util.stdlib.decodeToString
import kotlin.jvm.JvmInline

sealed interface RawMessage {

    val byteArrayView: ByteArrayView

    data class Text(val text: String) : RawMessage {

        companion object {

            fun of(bytes: ByteArrayView) = Text(bytes.decodeToString())
        }

        override val byteArrayView by lazy { ByteArrayView(text.encodeToByteArray()) }
    }

    @JvmInline
    value class Binary(override val byteArrayView: ByteArrayView) : RawMessage
}
