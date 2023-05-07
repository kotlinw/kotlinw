package kotlinw.remoting.core.codec

import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.codec.MessageDecoderMetadataPrefetchSupport.ExtractedMetadata
import kotlinw.util.stdlib.copyInto
import kotlinw.util.stdlib.readFromByteArray
import kotlinw.util.stdlib.readFromByteArrayView
import kotlinw.util.stdlib.view
import kotlinw.util.stdlib.writeToByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import okio.BufferedSource
import kotlin.jvm.JvmInline

@Serializable
private data class BinaryMessageHeader(
    val payloadSize: Int,
    val metadata: RemotingMessageMetadata?
)

@JvmInline
value class BinaryMessageCodecWithMetadataPrefetchSupport(
    private val wrappedCodec: KotlinxSerializationMessageCodec<RawMessage.Binary>
) : MessageCodecWithMetadataPrefetchSupport<RawMessage.Binary> {

    override val isBinary get() = true

    override val contentType get() = KotlinxSerializationBinaryMessageCodec.defaultBinaryContentType

    override fun <T> encode(message: T, serializer: KSerializer<T>): RawMessage.Binary =
        wrappedCodec.encode(message, serializer)

    override fun <T> encodeMessage(
        message: RemotingMessage<T>,
        payloadSerializer: KSerializer<T>
    ): RawMessage.Binary {
        val rawPayload = encode(message.payload, payloadSerializer)
        val payloadBytes = rawPayload.byteArrayView
        val payloadByteSize = payloadBytes.size

        val rawHeader =
            encode(BinaryMessageHeader(payloadByteSize, message.metadata), serializer<BinaryMessageHeader>())
        val headerBytes = rawHeader.byteArrayView
        val headerByteSize = headerBytes.size

        val messageBytes = ByteArray(Int.SIZE_BYTES + headerByteSize + payloadByteSize)
        headerByteSize.writeToByteArray(messageBytes, 0)
        headerBytes.copyInto(messageBytes, Int.SIZE_BYTES)
        payloadBytes.copyInto(messageBytes, Int.SIZE_BYTES + headerByteSize)

        return RawMessage.Binary(messageBytes.view())
    }

    override fun <T> decode(rawMessage: RawMessage.Binary, deserializer: KSerializer<T>): T =
        wrappedCodec.decode(rawMessage, deserializer)

    override fun <T> decodeMessage(
        rawMessage: RawMessage.Binary,
        payloadDeserializer: KSerializer<T>
    ): RemotingMessage<T> =
        extractMetadata(rawMessage).let {
            RemotingMessage(it.decodePayload(payloadDeserializer), it.metadata)
        }

    override fun extractMetadata(rawMessage: RawMessage.Binary): ExtractedMetadata {
        val sourceBytes = rawMessage.byteArrayView

        var offset = 0

        check(sourceBytes.size > offset + 4)
        val headerSize = Int.readFromByteArrayView(sourceBytes, offset)
        offset += 4

        check(sourceBytes.size > offset + headerSize)
        val headerBytes = sourceBytes.view(offset, offset + headerSize)
        offset += headerSize

        val header = decode(RawMessage.Binary(headerBytes), serializer<BinaryMessageHeader>())

        val payloadSize = header.payloadSize
        check(sourceBytes.size == offset + payloadSize)

        return object : ExtractedMetadata {

            override val metadata get() = header.metadata

            override fun <T> decodePayload(deserializer: KSerializer<T>): T {
                val payloadBytes = sourceBytes.view(offset, offset + payloadSize)
                return decode(RawMessage.Binary(payloadBytes), deserializer)
            }
        }
    }

    fun <T> decodeMessage(
        messageSource: BufferedSource,
        payloadDeserializer: KSerializer<T>
    ): RemotingMessage<T> =
        extractMetadata(messageSource).let {
            RemotingMessage(it.decodePayload(payloadDeserializer), it.metadata)
        }

    fun extractMetadata(messageSource: BufferedSource): ExtractedMetadata {
        val headerSize = Int.readFromByteArray(messageSource.readByteArray(Int.SIZE_BYTES.toLong()), 0)

        val headerBytes = messageSource.readByteArray(headerSize.toLong())
        val header = decode(RawMessage.Binary(headerBytes.view()), serializer<BinaryMessageHeader>())

        val payloadBytes = messageSource.readByteArray(header.payloadSize.toLong())

        return object : ExtractedMetadata {

            override val metadata get() = header.metadata

            override fun <T> decodePayload(deserializer: KSerializer<T>): T {
                return decode(RawMessage.Binary(payloadBytes.view()), deserializer)
            }
        }
    }
}
