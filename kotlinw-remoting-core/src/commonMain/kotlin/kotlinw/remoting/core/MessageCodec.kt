package kotlinw.remoting.core

import kotlinx.serialization.KSerializer

interface MessageCodecDescriptor {

    val contentType: String

    val isBinary: Boolean
}

interface MessageDecoder<in M : RawMessage> : MessageCodecDescriptor {

    fun <T : Any> decodeMessage(rawMessage: M, payloadDeserializer: KSerializer<T>): RemotingMessage<T>
}

interface MessageEncoder<out M : RawMessage> : MessageCodecDescriptor {

    fun <T : Any> encodeMessage(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>): M
}

interface MessageCodec<M : RawMessage> : MessageEncoder<M>, MessageDecoder<M> {
}

interface MessageDecoderMetadataPrefetchSupport<M : RawMessage> : MessageDecoder<M> {

    interface PrefetchedMetadata<T : Any> {

        val metadata: RemotingMessageMetadata?

        fun decodePayload(): T
    }

    fun <T : Any> prefetchMetadata(rawMessage: M, deserializer: KSerializer<T>): PrefetchedMetadata<T>
}
