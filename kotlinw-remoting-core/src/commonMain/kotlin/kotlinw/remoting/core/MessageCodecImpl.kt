package kotlinw.remoting.core

import kotlinw.remoting.server.core.RawMessage
import kotlinw.remoting.server.core.MessageCodec
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat

class MessageCodecImpl(
    override val descriptor: MessageCodecDescriptor
) : MessageCodecImplementor {

    private val serialFormat = descriptor.serialFormat

    init {
        require(serialFormat is StringFormat || serialFormat is BinaryFormat)
    }

    override fun <T : Any> decodeMessage(
        rawMessage: RawMessage,
        payloadDeserializer: KSerializer<T>
    ): T {
        val requestDeserializer = RemotingMessageSerializer(payloadDeserializer)
        return when (serialFormat) {
            is StringFormat -> serialFormat.decodeFromString(
                requestDeserializer,
                (rawMessage as RawMessage.Text).text
            ).payload

            is BinaryFormat -> serialFormat.decodeFromByteArray(
                requestDeserializer,
                (rawMessage as RawMessage.Binary).byteArray
            ).payload

            else -> throw IllegalStateException()
        }
    }

    override fun <T : Any> encodeMessage(payload: T, payloadSerializer: KSerializer<T>): RawMessage {
        val responseSerializer = RemotingMessageSerializer(payloadSerializer)
        val callResponse = RemotingMessage(payload, null) // TODO
        return when (serialFormat) {
            is StringFormat -> RawMessage.Text(serialFormat.encodeToString(responseSerializer, callResponse))
            is BinaryFormat -> RawMessage.Binary(serialFormat.encodeToByteArray(responseSerializer, callResponse))
            else -> throw IllegalStateException()
        }
    }
}
