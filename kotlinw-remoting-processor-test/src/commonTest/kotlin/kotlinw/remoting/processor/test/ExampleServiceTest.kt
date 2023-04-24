package kotlinw.remoting.processor.test

import io.ktor.client.engine.mock.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorHttpRemotingClient
import kotlinw.remoting.core.RemoteCallRequestSerializer
import kotlinw.remoting.core.RemoteCallResponse
import kotlinw.remoting.core.RemotingServerDelegateHelperImpl
import kotlinw.remoting.server.core.RemotingServerDelegate.Payload
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleServiceTest {

    @Test
    fun testWithMockResponse() {
        val serializer = Json.Default

        val mockEngine = MockEngine { request ->
            assertEquals("http://localhost/ExampleService/p1IntReturnsString_3", request.url.toString())
            assertEquals(ContentType.Application.Json, request.body.contentType)

            val requestBody = serializer.decodeFromString(
                RemoteCallRequestSerializer(serializer<ExampleServiceClientProxy.Parameter_p1IntReturnsString_3>()),
                (request.body as TextContent).text
            )
            assertEquals(ExampleServiceClientProxy.Parameter_p1IntReturnsString_3(123), requestBody.payload)

            respond(
                content = serializer.encodeToString(
                    RemoteCallResponse(
                        ExampleServiceClientProxy.Result_p1IntReturnsString_3(requestBody.payload.p1.toString()), null
                    )
                ),
                headers = buildHeaders {
                    set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
        }

        val remotingClient = KtorHttpRemotingClient(
            Json.Default,
            ContentType.Application.Json,
            mockEngine
        )
        val proxy = ExampleServiceClientProxy(remotingClient)

        runTest {
            assertEquals("123", proxy.p1IntReturnsString(123))
        }
    }

    @Test
    fun testWithRealResponse() {
        val target = mockk<ExampleService>(relaxed = true)
        coEvery { target.p1IntReturnsString(123) } returns "123"

        val serializer = Json.Default
        val delegateHelper = RemotingServerDelegateHelperImpl(serializer)
        val delegate = ExampleServiceServerDelegate(target, delegateHelper)

        val mockEngine = MockEngine { request ->
            assertEquals("http://localhost/ExampleService/p1IntReturnsString_3", request.url.toString())
            assertEquals(ContentType.Application.Json, request.body.contentType)

            val responseData = delegate.processCall(
                "p1IntReturnsString_3",
                Payload.Text((request.body as TextContent).text)
            )
            respond(
                content = (responseData as Payload.Text).text,
                headers = buildHeaders {
                    set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
        }

        val remotingClient = KtorHttpRemotingClient(
            Json.Default,
            ContentType.Application.Json,
            mockEngine
        )
        val proxy = ExampleServiceClientProxy(remotingClient)

        runTest {
            assertEquals("123", proxy.p1IntReturnsString(123))
        }

        coVerify {
            target.p1IntReturnsString(123)
        }
    }
}
