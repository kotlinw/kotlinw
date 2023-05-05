package kotlinw.remoting.core.codec

import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.ServiceLocator
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonMessageCodecTest {

    @Serializable
    private data class Payload(val number: Int)

    @Test
    fun testMetadataExtraction() {
        val payload = Payload(13)
        val metadata = RemotingMessageMetadata(
            timestamp = Clock.System.now(),
            serviceLocator = ServiceLocator("serviceId", "memberId"),
            messageKind = RemotingMessageKind.CallRequest("test")
        )
        val message = RemotingMessage(payload, metadata)

        val codec = JsonMessageCodec()
        val rawMessage = codec.encodeMessage(message, serializer())

        val extractedMetadata = codec.extractMetadata(rawMessage)
        assertEquals(metadata, extractedMetadata.metadata)
        assertEquals(payload, extractedMetadata.decodePayload(serializer()))
    }
}
