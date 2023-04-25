package kotlinw.remoting.server.ktor

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorRemotingHttpClientImplementor
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.core.PayloadSerializer
import kotlinw.remoting.core.MessageSerializerImpl
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.ExampleServiceClientProxy
import kotlinw.remoting.processor.test.ExampleServiceServerDelegate
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

fun PayloadSerializer.Companion.TextPayloadSerializer(
    contentType: ContentType,
    serialFormat: StringFormat
): PayloadSerializer.TextPayloadSerializer =
    PayloadSerializer.TextPayloadSerializer(contentType.toString(), serialFormat)

class KtorSupportTest {

    @Test
    fun test() = testApplication {
        val serializer = Json.Default

        val service = mockk<ExampleService>(relaxed = true)
        coEvery { service.p1IntReturnsString(any()) } returns "abc"

        val helper = MessageSerializerImpl(serializer)
        val payloadSerializer = PayloadSerializer.TextPayloadSerializer(ContentType.Application.Json, Json.Default)

        routing {
            remotingServerRouting(payloadSerializer, listOf(ExampleServiceServerDelegate(service, helper)))
        }

        val remotingHttpClientImplementor = KtorRemotingHttpClientImplementor(client)
        val remotingClient =
            HttpRemotingClient(serializer, ContentType.Application.Json.toString(), remotingHttpClientImplementor, "")

        val clientProxy = ExampleServiceClientProxy(remotingClient)
        assertEquals("abc", clientProxy.p1IntReturnsString(123))

        coVerify {
            service.p1IntReturnsString(123)
        }
    }
}
