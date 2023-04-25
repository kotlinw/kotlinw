package kotlinw.remoting.core

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

sealed interface MessageCodecDescriptor {

    companion object

    val contentType: String

    val serialFormat: SerialFormat

    val isBinary: Boolean

    class Text(override val contentType: String, override val serialFormat: StringFormat) :
        MessageCodecDescriptor {

        override val isBinary get() = false
    }

    class Binary(override val serialFormat: BinaryFormat) : MessageCodecDescriptor {

        override val contentType get() = "application/octet-stream"

        override val isBinary get() = true
    }
}
