package kotlinw.serialization.core.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toDuration

@Serializable
@SerialName("Duration")
private class DurationSurrogate(val seconds: Long, val nanoseconds: Int)

// TODO delete if official serializer arrives
// TODO @Serializer(forClass = Duration::class)
object DurationSerializer : KSerializer<Duration> {

    override val descriptor = DurationSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeSerializableValue(
            DurationSurrogate.serializer(),
            value.toComponents { seconds, nanoseconds -> DurationSurrogate(seconds, nanoseconds) })
    }

    override fun deserialize(decoder: Decoder): Duration {
        val surrogate = decoder.decodeSerializableValue(DurationSurrogate.serializer())
        return surrogate.seconds.toDuration(SECONDS) + surrogate.nanoseconds.toDuration(NANOSECONDS)
    }
}
