// TODO move to commonTest
package kotlinw.remoting.core

import arrow.core.some
import kotlinw.remoting.api.SupportsRemoting
import kotlinw.remoting.api.client.proxy
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.Okio
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.sink
import okio.source
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class StreamBasedSynchronousRemotingTest {

    private class PipedSourceSinkPair {

        private val inputStream = PipedInputStream()

        private val outputStream = PipedOutputStream(inputStream)

        val source = inputStream.source().buffer()

        val sink = outputStream.sink().buffer()
    }

    private class EchoServiceImpl : EchoService {

        override suspend fun echo(message: String): String = message
    }

    @Test
    fun testEcho() {
        runBlocking {
            val messageCodec = BinaryMessageCodec(
                GenericTextMessageCodec(Json, "application/json").asBinaryMessageCodec()
            )

            val serverToClientPipe = PipedSourceSinkPair()
            val clientToServerPipe = PipedSourceSinkPair()

            launch(newSingleThreadContext("server")) {
                StreamBasedSynchronousRemotingServer(
                    messageCodec,
                    listOf(EchoServiceRemoteCallDelegator(EchoServiceImpl())),
                    clientToServerPipe.source,
                    serverToClientPipe.sink
                ).listen()
            }

            launch {
                val remotingClient = StreamBasedSynchronousRemotingClient(
                    messageCodec,
                    serverToClientPipe.source,
                    clientToServerPipe.sink
                )
                val proxy = remotingClient.proxy(::EchoServiceClientProxy)
                assertEquals("Hello!!!", proxy.echo("Hello!!!"))
            }
        }
    }
}
