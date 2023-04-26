package kotlinw.remoting.core

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat

sealed class GenericMessageCodec<M : RawMessage> : MessageCodec<M> {
}

class GenericTextMessageCodec(
    private val serialFormat: StringFormat,
    override val contentType: String,
) : GenericMessageCodec<RawMessage.Text>() {

    override val isBinary = false

    override fun <T : Any> decodeMessage(
        rawMessage: RawMessage.Text,
        payloadDeserializer: KSerializer<T>
    ): RemotingMessage<T> =
        serialFormat.decodeFromString(RemotingMessageSerializer(payloadDeserializer), rawMessage.text)

    override fun <T : Any> encodeMessage(
        message: RemotingMessage<T>,
        payloadSerializer: KSerializer<T>
    ): RawMessage.Text =
        RawMessage.Text(serialFormat.encodeToString(RemotingMessageSerializer(payloadSerializer), message))
}

class GenericBinaryMessageCodec(
    private val serialFormat: BinaryFormat,
    override val contentType: String = defaultBinaryContentType
) : GenericMessageCodec<RawMessage.Binary>() {

    companion object {

        const val defaultBinaryContentType: String = "application/octet-stream"
    }

    override val isBinary = true

    override fun <T : Any> decodeMessage(
        rawMessage: RawMessage.Binary,
        payloadDeserializer: KSerializer<T>
    ): RemotingMessage<T> =
        serialFormat.decodeFromByteArray(RemotingMessageSerializer(payloadDeserializer), rawMessage.byteArray)

    override fun <T : Any> encodeMessage(
        message: RemotingMessage<T>,
        payloadSerializer: KSerializer<T>
    ): RawMessage.Binary =
        RawMessage.Binary(serialFormat.encodeToByteArray(RemotingMessageSerializer(payloadSerializer), message))
}
