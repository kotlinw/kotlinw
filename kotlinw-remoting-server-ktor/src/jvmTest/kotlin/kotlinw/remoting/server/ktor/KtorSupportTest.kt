package kotlinw.remoting.server.ktor

import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorRemotingHttpClientImplementor
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.core.PayloadSerializer
import kotlinw.remoting.core.RemotingServerDelegateHelperImpl
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.ExampleServiceClientProxy
import kotlinw.remoting.processor.test.ExampleServiceServerDelegate
import kotlinw.remoting.server.core.RemotingServerDelegate
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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

        val helper = RemotingServerDelegateHelperImpl(serializer)
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
