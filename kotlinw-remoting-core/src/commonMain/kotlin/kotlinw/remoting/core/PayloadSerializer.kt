package kotlinw.remoting.core

import kotlinw.remoting.server.core.RemotingServerDelegate
import kotlinw.remoting.server.core.RemotingServerDelegate.Payload
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

sealed interface PayloadSerializer<out T : Payload, out S : SerialFormat> {

    companion object

    val contentType: String

    val serialFormat: S

    class TextPayloadSerializer(override val contentType: String, override val serialFormat: StringFormat) :
        PayloadSerializer<Payload.Text, StringFormat>

    class BinaryPayloadSerializer(override val serialFormat: BinaryFormat) :
        PayloadSerializer<Payload.Binary, BinaryFormat> {

        override val contentType get() = "application/octet-stream"
    }
}
