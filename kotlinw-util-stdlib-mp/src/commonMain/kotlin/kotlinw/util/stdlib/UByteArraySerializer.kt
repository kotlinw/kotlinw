package kotlinw.util.stdlib

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@OptIn(ExperimentalUnsignedTypes::class)
object UByteArraySerializer : KSerializer<UByteArray> {

    override val descriptor = UByteArraySurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: UByteArray) {
        encoder.encodeSerializableValue(
            UByteArraySurrogate.serializer(),
            UByteArraySurrogate(value.map { it.toUInt() }.toUIntArray().asIntArray())
        )
    }

    override fun deserialize(decoder: Decoder) =
        decoder
            .decodeSerializableValue(UByteArraySurrogate.serializer())
            .intArray
            .map { it.toUByte() }
            .toUByteArray()
}

@Serializable
@SerialName("UByteArray")
@JvmInline
private value class UByteArraySurrogate(val intArray: IntArray) {

    override fun toString() = intArray.toString()
}
