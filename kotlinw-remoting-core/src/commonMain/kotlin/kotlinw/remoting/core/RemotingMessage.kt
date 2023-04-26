package kotlinw.remoting.core

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer

@Serializable
data class RemotingMessageMetadata(
    val timestamp: Instant? = null,
    val serviceLocator: ServiceLocator? = null
)

@Serializable(with = RemotingMessageSerializer::class)
data class RemotingMessage<T : Any>(
    val payload: T,
    val metadata: RemotingMessageMetadata?
)

class RemotingMessageSerializer<T : Any>(private val payloadSerializer: KSerializer<T>) :
    KSerializer<RemotingMessage<T>> {

    private val metadataSerializer = serializer<RemotingMessageMetadata?>()

    override val descriptor =
        buildClassSerialDescriptor("Request") {
            element("payload", payloadSerializer.descriptor)
            element<RemotingMessageMetadata?>("metadata")
        }

    override fun deserialize(decoder: Decoder): RemotingMessage<T> =
        decoder.decodeStructure(descriptor) {
            var payload: T? = null
            var metadata: RemotingMessageMetadata? = null
            if (decodeSequentially()) {
                payload = decodeSerializableElement(descriptor, 0, payloadSerializer)
                metadata = decodeSerializableElement(descriptor, 1, metadataSerializer)
            } else {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> payload = decodeSerializableElement(descriptor, 0, payloadSerializer)
                        1 -> metadata = decodeSerializableElement(descriptor, 1, metadataSerializer)
                        DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
            }

            RemotingMessage(payload!!, metadata)
        }

    override fun serialize(encoder: Encoder, value: RemotingMessage<T>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, payloadSerializer, value.payload)
            encodeSerializableElement(descriptor, 1, metadataSerializer, value.metadata)
        }
    }
}
