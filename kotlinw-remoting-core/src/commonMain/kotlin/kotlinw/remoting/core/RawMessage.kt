package kotlinw.remoting.core

import kotlinw.util.stdlib.ByteArrayView
import kotlinw.util.stdlib.ByteArrayView.Companion.decodeToString
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlin.jvm.JvmInline

sealed interface RawMessage {

    val byteArrayView: ByteArrayView

    data class Text(val text: String) : RawMessage {

        companion object {

            fun of(bytes: ByteArrayView) = Text(bytes.decodeToString())
        }

        override val byteArrayView by lazy {
            @Suppress("OPT_IN_USAGE")
            text.encodeToByteArray().view()
        }
    }

    @JvmInline
    value class Binary(override val byteArrayView: ByteArrayView) : RawMessage
}
