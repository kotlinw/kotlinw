package kotlinw.remoting.server.ktor

import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.client.WebRequestRemotingClientImpl
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.common.MutableRemotePeerRegistryImpl
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.ExampleServiceWithDownstreamFlows
import kotlinw.remoting.processor.test.clientProxy
import kotlinw.remoting.processor.test.remoteCallHandler
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

class KtorSupportTest {

    @Test
    fun testSynchronousCall() = testApplication {
        val service = mockk<ExampleService>(relaxed = true)
        coEvery { service.p1IntReturnsString(any()) } returns "abc"
        coEvery { service.noParameterReturnsNullableString() } returns null

        val messageCodec = JsonMessageCodec.Default

        install(WebSockets)
        install(RemotingServerPlugin) {
            this.remotingConfigurations = listOf(
                WebRequestRemotingConfiguration(
                    "test",
                    WebRequestRemotingProvider(PlatformLogging),
                    listOf(ExampleService.remoteCallHandler(service)),
                    null,
                    { null }, // TODO
                    { 1 } // TODO ezt ne kelljen már megadni, ha nincs authentikáció - külön class-ba kellene tenni ezeket
                )
            )
        }

        val remotingHttpClientImplementor = KtorHttpRemotingClientImplementor(client, PlatformLogging)
        val remotingClient =
            WebRequestRemotingClientImpl(
                messageCodec,
                remotingHttpClientImplementor,
                Url(""),
                PlatformLogging
            )

        val clientProxy = ExampleService.clientProxy(remotingClient)

        assertEquals("abc", clientProxy.p1IntReturnsString(123))
        assertNull(clientProxy.noParameterReturnsNullableString())

        coVerify {
            service.p1IntReturnsString(123)
            service.noParameterReturnsNullableString()
        }
    }

    @Test
    fun testFlowReturnType() = testApplication {
        val service = mockk<ExampleServiceWithDownstreamFlows>(relaxed = true)
        coEvery { service.coldFlow() } returns flowOf(1.0, 2.0, 3.0)
        coEvery { service.numberFlow(any()) } answers {
            flow {
                (1..5).forEach {
                    emit(arg<Int>(0) + it)
                }
            }
        }
        coEvery { service.nullableFlow() } returns flowOf("a", null, "b", null)

        val messageCodec = JsonMessageCodec.Default

        install(WebSockets)
        install(RemotingServerPlugin) {
            this.remotingConfigurations = listOf(
                WebSocketRemotingConfiguration(
                    "test",
                    WebSocketRemotingProvider(PlatformLogging, null, null),
                    listOf(ExampleServiceWithDownstreamFlows.remoteCallHandler(service)),
                    null,
                    { null }, // TODO
                    { 1 }// TODO ezt ne kelljen már megadni, ha nincs authentikáció - külön class-ba kellene tenni ezeket
                )
            )
        }

        val httpClient = createClient {
            install(ClientWebSockets)
        }

        val remotingHttpClientImplementor = KtorHttpRemotingClientImplementor(httpClient, PlatformLogging)
        val remotingClient =
            WebSocketRemotingClientImpl(
                messageCodec,
                remotingHttpClientImplementor,
                MutableRemotePeerRegistryImpl(PlatformLogging),
                Url(""),
                "test",
                emptySet(),
                PlatformLogging,
                currentCoroutineContext()
            )

        remotingClient.launch(context = CoroutineName("runMessagingLoop"), start = UNDISPATCHED) {
            remotingClient.connect()
        }

        remotingClient.withConnection {
            val clientProxy = ExampleServiceWithDownstreamFlows.clientProxy(it)
            assertEquals(listOf(1.0, 2.0, 3.0), clientProxy.coldFlow().toList())
            assertEquals(listOf(5, 6, 7, 8, 9), clientProxy.numberFlow(4).toList())
            assertEquals(listOf("a", null, "b", null), clientProxy.nullableFlow().toList())
        }

        remotingClient.close()

        coVerify {
            service.coldFlow()
            service.numberFlow(4)
            service.nullableFlow()
        }
    }
}
