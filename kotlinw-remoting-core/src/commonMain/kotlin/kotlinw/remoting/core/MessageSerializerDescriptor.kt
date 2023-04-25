package kotlinw.remoting.core

import kotlinw.remoting.server.core.RawMessage
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

sealed interface MessageSerializerDescriptor {

    companion object

    val contentType: String

    val serialFormat: SerialFormat

    val isText: Boolean

    class Text(override val contentType: String, override val serialFormat: StringFormat) :
        MessageSerializerDescriptor {

        override val isText get() = true
    }

    class Binary(override val serialFormat: BinaryFormat) : MessageSerializerDescriptor {

        override val contentType get() = "application/octet-stream"

        override val isText get() = false
    }
}
