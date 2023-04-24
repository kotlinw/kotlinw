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
data class RemoteCallRequestMetadata(
    val timestamp: Instant
)

@Serializable(with = RemoteCallRequestSerializer::class)
data class RemoteCallRequest<T: Any>(
    val payload: T,
    val metadata: RemoteCallRequestMetadata?
)

typealias RemoteCallRequestFactory<T> = (parameter: T) -> RemoteCallRequest<T>

class RemoteCallRequestSerializer<T: Any>(private val payloadSerializer: KSerializer<T>) :
    KSerializer<RemoteCallRequest<T>> {

    private val metadataSerializer = serializer<RemoteCallRequestMetadata?>()

    override val descriptor =
        buildClassSerialDescriptor("Request") {
            element("payload", payloadSerializer.descriptor)
            element<RemoteCallRequestMetadata?>("metadata")
        }

    override fun deserialize(decoder: Decoder): RemoteCallRequest<T> =
        decoder.decodeStructure(descriptor) {
            var payload: T? = null
            var metadata: RemoteCallRequestMetadata? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> payload = decodeSerializableElement(descriptor, 0, payloadSerializer)
                    1 -> metadata = decodeSerializableElement(descriptor, 1, metadataSerializer)
                    DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            RemoteCallRequest(payload!!, metadata)
        }

    override fun serialize(encoder: Encoder, value: RemoteCallRequest<T>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, payloadSerializer, value.payload)
            encodeSerializableElement(descriptor, 1, metadataSerializer, value.metadata)
        }
    }
}
