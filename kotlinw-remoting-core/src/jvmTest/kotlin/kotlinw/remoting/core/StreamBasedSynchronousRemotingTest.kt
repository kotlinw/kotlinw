// TODO move to commonTest
package kotlinw.remoting.core

import kotlinw.remoting.core.client.StreamBasedSynchronousRemotingClient
import kotlinw.remoting.core.codec.BinaryMessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.codec.KotlinxSerializationTextMessageCodec
import kotlinw.remoting.core.codec.asBinaryMessageCodec
import kotlinw.remoting.core.server.StreamBasedSynchronousRemotingServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.Pipe
import okio.buffer
import java.io.InterruptedIOException
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
            val messageCodec = BinaryMessageCodecWithMetadataPrefetchSupport(
                KotlinxSerializationTextMessageCodec(Json, "application/json").asBinaryMessageCodec()
            )

            val serverToClientPipe = Pipe(1000)
            val clientToServerPipe = Pipe(1000)

            val clientToServerPipeSource = clientToServerPipe.source
            clientToServerPipeSource.timeout().timeout(1, TimeUnit.SECONDS)

            launch(newSingleThreadContext("server")) {
                assertFailsWith(InterruptedIOException::class) {
                    StreamBasedSynchronousRemotingServer(
                        messageCodec,
                        listOf(EchoService.remoteCallDelegator(EchoServiceImpl())),
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
            val proxy = EchoService.clientProxy(remotingClient)
            assertEquals("Hello!!!", proxy.echo("Hello!!!"))
            assertEquals("Hello World!", proxy.echo("Hello World!"))
        }
    }
}
