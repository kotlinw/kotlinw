package kotlinw.remoting.server.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorHttpRemotingClient
import kotlinw.remoting.core.RemotingServerDelegateHelperImpl
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.ExampleServiceClientProxy
import kotlinw.remoting.processor.test.ExampleServiceServerDelegate
import kotlinw.remoting.server.core.RemotingServerDelegate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class KtorSupportTest {

    @Test
    fun test() = testApplication {
        val serializer = Json.Default

        routing {
            val service = mockk<ExampleService>(relaxed = true)
            coEvery { service.p1IntReturnsString(123) } returns "abc"

            val helper = RemotingServerDelegateHelperImpl(serializer)
            val delegate = ExampleServiceServerDelegate(service, helper)

            post("/{serviceName}/{methodName}") {
                val serviceName = call.parameters["serviceName"]
                assertEquals("ExampleService", serviceName)

                val methodName = call.parameters["methodName"] ?: fail()
                delegate.processCall(methodName, RemotingServerDelegate.Payload.Text(call.receiveText()))
            }
        }

        val clientProxy =
            ExampleServiceClientProxy(KtorHttpRemotingClient(serializer, ContentType.Application.Json, client))
        assertEquals("abc", clientProxy.p1IntReturnsString(123))
    }
}
