package kotlinw.remoting.core

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer

@Serializable
data class RemoteCallResponseMetadata(
    val timestamp: Instant
)

@Serializable(with = RemoteCallResponseSerializer::class)
data class RemoteCallResponse<T: Any>(
    val payload: T,
    val metadata: RemoteCallResponseMetadata?
)

typealias RemoteCallResponseFactory<T> = (parameter: T) -> RemoteCallResponse<T>

class RemoteCallResponseSerializer<T: Any>(private val payloadSerializer: KSerializer<T>) :
    KSerializer<RemoteCallResponse<T>> {

    private val metadataSerializer = serializer<RemoteCallResponseMetadata?>()

    override val descriptor =
        buildClassSerialDescriptor("Response") {
            element("payload", payloadSerializer.descriptor)
            element<RemoteCallResponseMetadata?>("metadata")
        }

    override fun deserialize(decoder: Decoder): RemoteCallResponse<T> =
        decoder.decodeStructure(descriptor) {
            var payload: T? = null
            var metadata: RemoteCallResponseMetadata? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> payload = decodeSerializableElement(descriptor, 0, payloadSerializer)
                    1 -> metadata = decodeSerializableElement(descriptor, 1, metadataSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            RemoteCallResponse(payload!!, metadata)
        }

    override fun serialize(encoder: Encoder, value: RemoteCallResponse<T>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, payloadSerializer, value.payload)
            encodeSerializableElement(descriptor, 1, metadataSerializer, value.metadata)
        }
    }
}
