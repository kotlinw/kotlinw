package kotlinw.remoting.core.codec

import kotlinw.remoting.core.RawMessage
import kotlinw.util.stdlib.decodeToString
import kotlinw.util.stdlib.toReadOnlyByteArray
import kotlinw.util.stdlib.view
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat

sealed class KotlinxSerializationMessageCodec<M : RawMessage> : MessageCodec<M>

class KotlinxSerializationTextMessageCodec(
    private val serialFormat: StringFormat,
    override val contentType: String,
) : KotlinxSerializationMessageCodec<RawMessage.Text>() {

    override val isBinary = false

    override fun <T> decode(rawMessage: RawMessage.Text, deserializer: KSerializer<T>): T =
        serialFormat.decodeFromString(deserializer, rawMessage.text)

    override fun <T> encode(message: T, serializer: KSerializer<T>): RawMessage.Text =
        RawMessage.Text(serialFormat.encodeToString(serializer, message))
}

class KotlinxSerializationBinaryMessageCodec(
    private val serialFormat: BinaryFormat,
    override val contentType: String = defaultBinaryContentType
) : KotlinxSerializationMessageCodec<RawMessage.Binary>() {

    companion object {

        const val defaultBinaryContentType: String = "application/octet-stream"
    }

    override val isBinary = true

    override fun <T> decode(rawMessage: RawMessage.Binary, deserializer: KSerializer<T>): T =
        serialFormat.decodeFromByteArray(deserializer, rawMessage.byteArrayView.toReadOnlyByteArray())

    override fun <T> encode(message: T, serializer: KSerializer<T>): RawMessage.Binary =
        RawMessage.Binary(serialFormat.encodeToByteArray(serializer, message).view())
}

private class KotlinxSerializationTextMessageCodecAsBinary(private val textCodec: KotlinxSerializationMessageCodec<RawMessage.Text>) :
    KotlinxSerializationMessageCodec<RawMessage.Binary>() {

    override val contentType = KotlinxSerializationBinaryMessageCodec.defaultBinaryContentType

    override val isBinary = true

    override fun <T> encode(message: T, serializer: KSerializer<T>): RawMessage.Binary =
        RawMessage.Binary(textCodec.encode(message, serializer).text.encodeToByteArray().view())

    override fun <T> decode(rawMessage: RawMessage.Binary, deserializer: KSerializer<T>): T =
        textCodec.decode(RawMessage.Text(rawMessage.byteArrayView.decodeToString()), deserializer)
}

fun KotlinxSerializationTextMessageCodec.asBinaryMessageCodec(): KotlinxSerializationMessageCodec<RawMessage.Binary> =
    KotlinxSerializationTextMessageCodecAsBinary(this)
