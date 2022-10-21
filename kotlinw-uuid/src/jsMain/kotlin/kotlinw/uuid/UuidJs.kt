package kotlinw.uuid

import kotlinw.immutator.annotation.Immutable
import kotlinw.js.ext.uuid.v4
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Immutable
actual data class Uuid(val value: String) {
    actual override fun toString() = value
}

actual object UuidSerializer : KSerializer<Uuid> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", STRING)

    override fun deserialize(decoder: Decoder): Uuid = Uuid(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.value)
    }
}

actual fun randomUuid(): Uuid = Uuid(v4())

actual fun uuidFromString(uuidString: String): Uuid = Uuid(uuidString)
