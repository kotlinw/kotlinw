package kotlinw.remoting.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class BinaryMessageHeader(
    val payloadSize: Int,
    val serviceLocator: ServiceLocator?
)

data class BinaryMessage<T : Any>(
    val serviceLocator: ServiceLocator?,
    val message: T
)

@JvmInline
value class BinaryMessageCodec(
    private val wrappedCodec: GenericBinaryMessageCodec
) : MessageCodec<RawMessage.Binary>, MessageDecoderMetadataPrefetchSupport<RawMessage.Binary> {

    override val isBinary get() = true

    override val contentType get() = GenericBinaryMessageCodec.defaultBinaryContentType

    override fun <T : Any> encodeMessage(
        message: RemotingMessage<T>,
        payloadSerializer: KSerializer<T>
    ): RawMessage.Binary {
        TODO("Not yet implemented")
    }

    override fun <T : Any> decodeMessage(
        rawMessage: RawMessage.Binary,
        payloadDeserializer: KSerializer<T>
    ): RemotingMessage<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> prefetchMetadata(
        rawMessage: RawMessage.Binary,
        deserializer: KSerializer<T>
    ): MessageDecoderMetadataPrefetchSupport.PrefetchedMetadata<T> {
        TODO("Not yet implemented")
    }
}
