package kotlinw.remoting.core

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

class MessageCodecImpl(
    private val serialFormat: SerialFormat,
    override val contentType: String,
    override val isBinary: Boolean
) : MessageCodec {

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
