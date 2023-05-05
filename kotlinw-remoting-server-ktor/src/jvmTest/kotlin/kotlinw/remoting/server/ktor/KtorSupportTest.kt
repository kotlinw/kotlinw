package kotlinw.remoting.server.ktor

import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.core.JsonMessageCodec
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.clientProxy
import kotlinw.remoting.processor.test.remoteCallDelegator
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.server.websocket.WebSockets.Plugin as ServerWebSockets

class KtorSupportTest {

    @Test
    fun testSynchronousCall() = testApplication {
        val service = mockk<ExampleService>(relaxed = true)
        coEvery { service.p1IntReturnsString(any()) } returns "abc"

        val messageCodec = JsonMessageCodec.Default

        install(ServerWebSockets)
        install(RemotingPlugin) {
            this.messageCodec = messageCodec
            this.remoteCallDelegators = listOf(ExampleService.remoteCallDelegator(service))
            this.identifyClient = { 1 }
        }

        val remotingHttpClientImplementor = KtorHttpRemotingClientImplementor(client)
        val remotingClient =
            HttpRemotingClient(messageCodec, remotingHttpClientImplementor, Url(""))

        val clientProxy = ExampleService.clientProxy(remotingClient)
        assertEquals("abc", clientProxy.p1IntReturnsString(123))

        coVerify {
            service.p1IntReturnsString(123)
        }
    }

    @Test
    fun testFlowReturnType() = testApplication {
        val service = mockk<ExampleService>(relaxed = true)
        coEvery { service.coldFlow() } returns flowOf(1.0, 2.0, 3.0)
        coEvery { service.numberFlow(any()) } answers {
            flow {
                (1..5).forEach {
                    emit(arg<Int>(0) + it)
                }
            }
        }

        val messageCodec = JsonMessageCodec.Default

        install(ServerWebSockets)
        install(RemotingPlugin)
        {
            this.messageCodec = messageCodec
            this.remoteCallDelegators = listOf(ExampleService.remoteCallDelegator(service))
            this.identifyClient = { 1 }
        }

        val httpClient = createClient {
            install(ClientWebSockets)
        }

        val remotingHttpClientImplementor = KtorHttpRemotingClientImplementor(httpClient)
        val remotingClient =
            HttpRemotingClient(messageCodec, remotingHttpClientImplementor, Url(""))
        val clientProxy = ExampleService.clientProxy(remotingClient)

        assertEquals(listOf(1.0, 2.0, 3.0), clientProxy.coldFlow().toList())

        assertEquals(listOf(5, 6, 7, 8, 9), clientProxy.numberFlow(4).toList())

        coVerify {
            service.coldFlow()
            service.numberFlow(4)
        }
    }
}
