package kotlinw.remoting.core

import kotlinw.remoting.server.core.RawMessage
import kotlinw.remoting.server.core.MessageSerializer
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

class MessageSerializerImpl(
    messageSerializerDescriptor: MessageSerializerDescriptor
) : MessageSerializer {

    private val serialFormat = messageSerializerDescriptor.serialFormat

    init {
        require(serialFormat is StringFormat || serialFormat is BinaryFormat)
    }

    override fun <T : Any> readMessage(
        requestData: RawMessage,
        payloadDeserializer: KSerializer<T>
    ): T {
        val requestDeserializer = RemoteCallRequestSerializer(payloadDeserializer)
        // TODO allow access to metadata
        return when (serialFormat) {
            is StringFormat -> serialFormat.decodeFromString(
                requestDeserializer,
                (requestData as RawMessage.Text).text
            ).payload

            is BinaryFormat -> serialFormat.decodeFromByteArray(
                requestDeserializer,
                (requestData as RawMessage.Binary).byteArray
            ).payload

            else -> throw IllegalStateException()
        }
    }

    override fun <T : Any> writeMessage(payload: T, payloadSerializer: KSerializer<T>): RawMessage {
        val responseSerializer = RemoteCallResponseSerializer(payloadSerializer)
        val callResponse = RemoteCallResponse(payload, null) // TODO
        return when (serialFormat) {
            is StringFormat -> RawMessage.Text(serialFormat.encodeToString(responseSerializer, callResponse))
            is BinaryFormat -> RawMessage.Binary(serialFormat.encodeToByteArray(responseSerializer, callResponse))
            else -> throw IllegalStateException()
        }
    }
}
