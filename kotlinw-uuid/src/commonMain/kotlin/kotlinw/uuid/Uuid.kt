package kotlinw.uuid

import kotlinw.immutator.annotation.Immutable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@JvmInline
@Immutable
@Serializable(with = UuidSerializer::class)
value class Uuid(val value: com.benasher44.uuid.Uuid) {

    companion object {

        fun randomUuid(): Uuid = Uuid(com.benasher44.uuid.uuid4())

        fun parseUuid(uuidString: String): Uuid = Uuid(com.benasher44.uuid.uuidFrom(uuidString))

        fun uuidOf(mostSignificantBits: Long, leastSignificantBits: Long): Uuid =
            Uuid(com.benasher44.uuid.Uuid(mostSignificantBits, leastSignificantBits))

        fun uuidOf(bytes: ByteArray): Uuid = Uuid(com.benasher44.uuid.uuidOf(bytes))
    }

    val leastSignificantBits: Long get() = value.leastSignificantBits

    val mostSignificantBits: Long get() = value.mostSignificantBits

    override fun toString() = value.toString()
}

object UuidSerializer : KSerializer<Uuid> {

    override val descriptor = PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.parseUuid(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }
}
