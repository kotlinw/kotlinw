package kotlinw.ulid

import de.huxhorn.sulky.ulid.SulkyUlid
import kotlinw.uuid.Uuid
import kotlinw.uuid.Uuid.Companion.uuidOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@Serializable(with = UlidSerializer::class)
@JvmInline
value class Ulid(val value: SulkyUlid.Value) {

    companion object {

        fun randomUlid(): Ulid = Ulid(SulkyUlid.nextValue())

        fun parseUlid(ulidString: String): Ulid = Ulid(SulkyUlid.parseULID(ulidString))

        fun nextMonotonicUlid(previousUlid: Ulid): Ulid = Ulid(SulkyUlid.nextMonotonicValue(previousUlid.value))

        fun nextStrictlyMonotonicUlidOrNull(previousUlid: Ulid): Ulid? =
            SulkyUlid.nextStrictlyMonotonicValue(previousUlid.value)?.let { Ulid(it) }

        fun ulidOf(mostSignificantBits: Long, leastSignificantBits: Long): Ulid =
            Ulid(SulkyUlid.Value(mostSignificantBits, leastSignificantBits))
    }

    override fun toString() = value.toString()
}

object UlidSerializer : KSerializer<Ulid> {

    override val descriptor = PrimitiveSerialDescriptor("Ulid", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Ulid {
        return Ulid.parseUlid(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Ulid) {
        encoder.encodeString(value.toString())
    }
}

fun Ulid.toUuid(): Uuid = uuidOf(value.mostSignificantBits, value.leastSignificantBits)

fun Uuid.toUlid(): Ulid = Ulid.ulidOf(value.mostSignificantBits, value.leastSignificantBits)
