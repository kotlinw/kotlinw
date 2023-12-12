package kotlinw.i18n

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

interface LocalizedTextKey

interface LocalizedTextKeyEnum<E> : LocalizedTextKey
    where E : Enum<E>, E : LocalizedTextKeyEnum<E>

val <E> E.localizedTextKey: String
        where E : Enum<E>, E : LocalizedTextKeyEnum<E>
    get() = name.split('_').joinToString(".") { it.replaceFirstChar { it.lowercase() } }

inline fun <reified E> localizedTextKeyMappings(): Map<String, E>
        where E : Enum<E>, E : LocalizedTextKeyEnum<E> =
    enumValues<E>().associateBy { it.localizedTextKey }

abstract class LocalizedTextKeySerializer<E>(
    localizedTextKeyEnum: KClass<E>,
    private val unknownKey: E,
    private val localizedTextKeyMappings: Map<String, E>
) : KSerializer<E>
        where E : Enum<E>, E : LocalizedTextKeyEnum<E> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(localizedTextKeyEnum.simpleName!!, STRING)

    override fun deserialize(decoder: Decoder): E = localizedTextKeyMappings[decoder.decodeString()] ?: unknownKey

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeString(value.localizedTextKey)
    }
}
