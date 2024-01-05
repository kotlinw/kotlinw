package kotlinw.remoting.client.ktor

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.utils.buildHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageSerializer
import kotlinw.remoting.core.client.WebRequestRemotingClientImpl
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class KtorHttpRemotingClientTest {

    @Serializable
    private data class TestParameter(val s: String)

    @Serializable
    private data class TestResult(val i: Int)

    private interface FakeService {

        suspend fun fakeMethod(parameter: TestParameter): TestResult
    }

    @Test
    fun testRequestResponse() {
        val serializer = Json.Default

        val mockEngine = MockEngine { request ->
            assertEquals("http://localhost/remoting/call/FakeService/fakeMethod", request.url.toString())
            assertEquals(ContentType.Application.Json, request.body.contentType)

            val requestPayload = serializer.decodeFromString(
                RemotingMessageSerializer(serializer<TestParameter>()),
                (request.body as TextContent).text
            )
            assertEquals(TestParameter("abc"), requestPayload.payload)

            respond(
                content = serializer.encodeToString(RemotingMessage(TestResult(123), null)),
                headers = buildHeaders {
                    set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
        }

        val remotingClient = WebRequestRemotingClientImpl(
            JsonMessageCodec.Default,
            KtorHttpRemotingClientImplementor(mockEngine),
            Url(""),
            PlatformLogging
        )

        runTest {
            assertEquals(
                TestResult(123),
                remotingClient.call(
                    FakeService::class,
                    FakeService::fakeMethod,
                    FakeService::class.simpleName!!,
                    FakeService::fakeMethod.name,
                    TestParameter("abc"),
                    serializer(),
                    serializer<TestResult>()
                )
            )
        }
    }
}
