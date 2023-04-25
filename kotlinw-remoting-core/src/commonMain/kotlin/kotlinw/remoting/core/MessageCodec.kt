package kotlinw.remoting.core

import kotlinx.serialization.KSerializer

interface MessageCodec {

    val contentType: String

    val isBinary: Boolean

    fun <T : Any> decodeMessage(rawMessage: RawMessage, payloadDeserializer: KSerializer<T>): T

    fun <T : Any> encodeMessage(payload: T, payloadSerializer: KSerializer<T>): RawMessage
}

interface MessageCodecMetadataPrefetchSupport: MessageCodec {

    interface PrefetchedMetadata {

        val metadata: RemotingMessageMetadata?

        fun <T: Any> decodePayload(payloadDeserializer: KSerializer<T>): T
    }

    fun prefetchMetadata(rawMessage: RawMessage): PrefetchedMetadata
}
