package kotlinw.remoting.core.codec

import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.RemotingMessageSerializer
import kotlinx.serialization.KSerializer

interface MessageCodecDescriptor {

    val contentType: String

    val isBinary: Boolean
}

interface MessageDecoder<in M : RawMessage> : MessageCodecDescriptor {

    fun <T : Any> decode(rawMessage: M, deserializer: KSerializer<T>): T

    fun <T : Any> decodeMessage(rawMessage: M, payloadDeserializer: KSerializer<T>): RemotingMessage<T> =
        decode(rawMessage, RemotingMessageSerializer(payloadDeserializer))
}

interface MessageEncoder<out M : RawMessage> : MessageCodecDescriptor {

    fun <T : Any> encode(message: T, serializer: KSerializer<T>): M

    fun <T : Any> encodeMessage(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>): M =
        encode(message, RemotingMessageSerializer(payloadSerializer))
}

interface MessageCodec<M : RawMessage> : MessageEncoder<M>, MessageDecoder<M> {
}

interface MessageDecoderMetadataPrefetchSupport<M : RawMessage> : MessageDecoder<M> {

    interface ExtractedMetadata {

        val metadata: RemotingMessageMetadata?

        fun <T : Any> decodePayload(deserializer: KSerializer<T>): T

        fun <T : Any> decodeMessage(deserializer: KSerializer<T>): RemotingMessage<T> =
            RemotingMessage(decodePayload(deserializer), metadata)
    }

    fun extractMetadata(rawMessage: M): ExtractedMetadata
}

interface MessageCodecWithMetadataPrefetchSupport<M : RawMessage> : MessageCodec<M>,
    MessageDecoderMetadataPrefetchSupport<M>
