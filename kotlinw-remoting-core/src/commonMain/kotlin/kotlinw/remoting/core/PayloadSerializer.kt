package kotlinw.remoting.core

import kotlinw.remoting.server.core.RawMessage
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

sealed interface PayloadSerializer<out T : RawMessage, out S : SerialFormat> {

    companion object

    val contentType: String

    val serialFormat: S

    class TextPayloadSerializer(override val contentType: String, override val serialFormat: StringFormat) :
        PayloadSerializer<RawMessage.Text, StringFormat>

    class BinaryPayloadSerializer(override val serialFormat: BinaryFormat) :
        PayloadSerializer<RawMessage.Binary, BinaryFormat> {

        override val contentType get() = "application/octet-stream"
    }
}
