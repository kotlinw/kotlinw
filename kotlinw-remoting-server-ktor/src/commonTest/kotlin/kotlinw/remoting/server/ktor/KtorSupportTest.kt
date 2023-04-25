package kotlinw.remoting.server.ktor

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorRemotingHttpClientImplementor
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.core.MessageSerializerImpl
import kotlinw.remoting.core.MessageSerializerDescriptor
import kotlinw.remoting.core.ktor.Text
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.ExampleServiceClientProxy
import kotlinw.remoting.processor.test.ExampleServiceRemoteCallDelegator
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorSupportTest {

    @Test
    fun test() = testApplication {
        val serializer = Json.Default

        val service = mockk<ExampleService>(relaxed = true)
        coEvery { service.p1IntReturnsString(any()) } returns "abc"

        val messageSerializerDescriptor = MessageSerializerDescriptor.Text(ContentType.Application.Json, Json)

        routing {
            remotingServerRouting(messageSerializerDescriptor, listOf(ExampleServiceRemoteCallDelegator(service)))
        }

        val remotingHttpClientImplementor = KtorRemotingHttpClientImplementor(client)
        val remotingClient =
            HttpRemotingClient(messageSerializerDescriptor, remotingHttpClientImplementor, "")

        val clientProxy = ExampleServiceClientProxy(remotingClient)
        assertEquals("abc", clientProxy.p1IntReturnsString(123))

        coVerify {
            service.p1IntReturnsString(123)
        }
    }
}
