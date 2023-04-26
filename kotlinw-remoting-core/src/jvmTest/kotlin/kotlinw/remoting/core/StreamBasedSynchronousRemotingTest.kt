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
import okio.Pipe
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.sink
import okio.source
import java.io.InterruptedIOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StreamBasedSynchronousRemotingTest {

    private class EchoServiceImpl : EchoService {

        override suspend fun echo(message: String): String = message
    }

    @Test
    fun testEcho() {
        runBlocking {
            val messageCodec = BinaryMessageCodec(
                GenericTextMessageCodec(Json, "application/json").asBinaryMessageCodec()
            )

            val serverToClientPipe = Pipe(1000)
            val clientToServerPipe = Pipe(1000)

            val clientToServerPipeSource = clientToServerPipe.source
            clientToServerPipeSource.timeout().timeout(1, TimeUnit.SECONDS)

            launch(newSingleThreadContext("server")) {
                assertFailsWith(InterruptedIOException::class) {
                    StreamBasedSynchronousRemotingServer(
                        messageCodec,
                        listOf(EchoServiceRemoteCallDelegator(EchoServiceImpl())),
                        clientToServerPipeSource.buffer(),
                        serverToClientPipe.sink.buffer()
                    ).listen()
                }
            }

            val remotingClient = StreamBasedSynchronousRemotingClient(
                messageCodec,
                serverToClientPipe.source.buffer(),
                clientToServerPipe.sink.buffer()
            )
            val proxy = remotingClient.proxy(::EchoServiceClientProxy)
            assertEquals("Hello!!!", proxy.echo("Hello!!!"))
            assertEquals("Hello World!", proxy.echo("Hello World!"))
        }
    }
}
