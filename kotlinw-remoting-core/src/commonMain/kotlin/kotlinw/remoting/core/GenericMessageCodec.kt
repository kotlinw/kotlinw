package kotlinw.remoting.core

import kotlinw.util.stdlib.decodeToString
import kotlinw.util.stdlib.toReadOnlyByteArray
import kotlinw.util.stdlib.view
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlin.io.encoding.Base64

sealed class GenericMessageCodec<M : RawMessage> : MessageCodec<M>

class GenericTextMessageCodec(
    private val serialFormat: StringFormat,
    override val contentType: String,
) : GenericMessageCodec<RawMessage.Text>() {

    override val isBinary = false

    override fun <T : Any> decode(rawMessage: RawMessage.Text, deserializer: KSerializer<T>): T =
        serialFormat.decodeFromString(deserializer, rawMessage.text)

    override fun <T : Any> encode(message: T, serializer: KSerializer<T>): RawMessage.Text =
        RawMessage.Text(serialFormat.encodeToString(serializer, message))
}

class GenericBinaryMessageCodec(
    private val serialFormat: BinaryFormat,
    override val contentType: String = defaultBinaryContentType
) : GenericMessageCodec<RawMessage.Binary>() {

    companion object {

        const val defaultBinaryContentType: String = "application/octet-stream"
    }

    override val isBinary = true

    override fun <T : Any> decode(
        rawMessage: RawMessage.Binary,
        deserializer: KSerializer<T>
    ): T =
        serialFormat.decodeFromByteArray(deserializer, rawMessage.byteArrayView.toReadOnlyByteArray())

    override fun <T : Any> encode(
        message: T,
        serializer: KSerializer<T>
    ): RawMessage.Binary =
        RawMessage.Binary(serialFormat.encodeToByteArray(serializer, message).view())
}

private class GenericTextMessageCodecAsBinary(private val textCodec: GenericMessageCodec<RawMessage.Text>) :
    GenericMessageCodec<RawMessage.Binary>() {

    override val contentType = GenericBinaryMessageCodec.defaultBinaryContentType

    override val isBinary = true

    override fun <T : Any> encode(message: T, serializer: KSerializer<T>): RawMessage.Binary =
        RawMessage.Binary(textCodec.encode(message, serializer).text.encodeToByteArray().view())

    override fun <T : Any> decode(rawMessage: RawMessage.Binary, deserializer: KSerializer<T>): T =
        textCodec.decode(RawMessage.Text(rawMessage.byteArrayView.decodeToString()), deserializer)
}

fun GenericTextMessageCodec.asBinaryMessageCodec(): GenericMessageCodec<RawMessage.Binary> =
    GenericTextMessageCodecAsBinary(this)
