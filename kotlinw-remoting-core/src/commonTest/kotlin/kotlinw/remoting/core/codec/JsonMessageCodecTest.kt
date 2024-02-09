package kotlinw.remoting.core.codec

import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.ServiceLocator
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.kotlinw.serialization.json.standardLongTermJson

class JsonMessageCodecTest {

    @Serializable
    private data class Payload(val number: Int)

    @Test
    fun testMetadataExtraction() = runTest {
        val payload = Payload(13)
        val metadata = RemotingMessageMetadata(
            timestamp = Clock.System.now(),
            messageKind = RemotingMessageKind.CallRequest("test", ServiceLocator("serviceId", "memberId"))
        )
        val message = RemotingMessage(payload, metadata)

        val codec = JsonMessageCodec(standardLongTermJson())
        val rawMessage = codec.encodeMessage(message, serializer())

        val extractedMetadata = codec.extractMetadata(rawMessage)
        assertEquals(metadata, extractedMetadata.metadata)
        assertEquals(payload, extractedMetadata.decodePayload(serializer()))
    }
}
