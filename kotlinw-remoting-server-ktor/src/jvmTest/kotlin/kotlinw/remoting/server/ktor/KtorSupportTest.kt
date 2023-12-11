package kotlinw.remoting.server.ktor

import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.clientProxy
import kotlinw.remoting.processor.test.remoteCallHandler
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinw.remoting.core.common.MutableRemotePeerRegistryImpl
import kotlinw.remoting.processor.test.ExampleServiceWithDownstreamFlows
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.coroutineScope
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
                    WebRequestRemotingProvider(),
                    listOf(ExampleService.remoteCallHandler(service)),
                    null
                )
            )
        }

        val remotingHttpClientImplementor = KtorHttpRemotingClientImplementor(client)
        val remotingClient =
            HttpRemotingClient(
                messageCodec,
                remotingHttpClientImplementor,
                MutableRemotePeerRegistryImpl(),
                Url(""),
                emptyMap()
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
                    WebSocketRemotingProvider({ 1 }, null, null),
                    listOf(ExampleServiceWithDownstreamFlows.remoteCallHandler(service)),
                    null
                )
            )
        }

        val httpClient = createClient {
            install(ClientWebSockets)
        }

        val remotingHttpClientImplementor = KtorHttpRemotingClientImplementor(httpClient)
        val remotingClient =
            HttpRemotingClient(
                messageCodec,
                remotingHttpClientImplementor,
                MutableRemotePeerRegistryImpl(),
                Url(""),
                emptyMap()
            )
        val clientProxy = ExampleServiceWithDownstreamFlows.clientProxy(remotingClient)

        coroutineScope {
            val loopJob = launch(start = UNDISPATCHED) {
                remotingClient.runMessagingLoop()
            }

            assertEquals(listOf(1.0, 2.0, 3.0), clientProxy.coldFlow().toList())
            assertEquals(listOf(5, 6, 7, 8, 9), clientProxy.numberFlow(4).toList())
            assertEquals(listOf("a", null, "b", null), clientProxy.nullableFlow().toList())
            loopJob.cancel()
        }

        coVerify {
            service.coldFlow()
            service.numberFlow(4)
            service.nullableFlow()
        }
    }
}
