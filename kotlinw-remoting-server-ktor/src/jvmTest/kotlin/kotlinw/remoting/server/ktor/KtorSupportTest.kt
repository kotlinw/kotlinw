package kotlinw.remoting.server.ktor

import io.ktor.http.ContentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorRemotingHttpClientImplementor
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.core.ktor.GenericTextMessageCodec
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.clientProxy
import kotlinw.remoting.processor.test.remoteCallDelegator
import kotlinw.util.stdlib.Url
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorSupportTest {

    @Test
    fun test() = testApplication {
        val service = mockk<ExampleService>(relaxed = true)
        coEvery { service.p1IntReturnsString(any()) } returns "abc"

        val messageCodec = GenericTextMessageCodec(Json, ContentType.Application.Json)

        routing {
            remotingServerRouting(messageCodec, listOf(ExampleService.remoteCallDelegator(service)))
        }

        val remotingHttpClientImplementor = KtorRemotingHttpClientImplementor(client)
        val remotingClient =
            HttpRemotingClient(messageCodec, remotingHttpClientImplementor, Url(""))

        val clientProxy = ExampleService.clientProxy(remotingClient)
        assertEquals("abc", clientProxy.p1IntReturnsString(123))

        coVerify {
            service.p1IntReturnsString(123)
        }
    }
}
