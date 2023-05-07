package kotlinw.remoting.core.codec

import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.codec.MessageDecoderMetadataPrefetchSupport.ExtractedMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class JsonMessageCodec(
    private val json: Json = Json {
        encodeDefaults = false
        explicitNulls = false
    }
) : MessageCodecWithMetadataPrefetchSupport<RawMessage.Text> {

    companion object {

        val Default = JsonMessageCodec()

        private val metadataPropertyName = RemotingMessage<*>::metadata.name

        private val payloadPropertyName = RemotingMessage<*>::payload.name
    }

    override val isBinary = false

    override fun extractMetadata(rawMessage: RawMessage.Text): ExtractedMetadata {
        val messageJsonObject = json.decodeFromString<JsonObject>(rawMessage.text)
        val payloadJsonElement = messageJsonObject[payloadPropertyName] ?: TODO()
        val metadata =
            messageJsonObject[metadataPropertyName]?.let { json.decodeFromJsonElement<RemotingMessageMetadata>(it) }

        return object : ExtractedMetadata {

            override val metadata get() = metadata

            override fun <T> decodePayload(deserializer: KSerializer<T>): T =
                json.decodeFromJsonElement(deserializer, payloadJsonElement)
        }
    }

    override fun <T> decode(rawMessage: RawMessage.Text, deserializer: KSerializer<T>): T =
        json.decodeFromString(deserializer, rawMessage.text)

    override fun <T> encode(message: T, serializer: KSerializer<T>): RawMessage.Text =
        RawMessage.Text(json.encodeToString(serializer, message))

    override val contentType: String = "application/json"
}
