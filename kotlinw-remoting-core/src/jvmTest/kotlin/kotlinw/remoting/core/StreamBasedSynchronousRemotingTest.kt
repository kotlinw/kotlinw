// TODO move to commonTest
package kotlinw.remoting.core

import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncOutputStream
import kotlinw.collection.LinkedQueue
import kotlinw.collection.MutableQueue
import kotlinw.remoting.core.client.StreamBasedSynchronousRemotingClient
import kotlinw.remoting.core.codec.BinaryMessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.codec.KotlinxSerializationTextMessageCodec
import kotlinw.remoting.core.codec.asBinaryMessageCodec
import kotlinw.remoting.core.server.StreamBasedSynchronousRemotingServer
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.InterruptedIOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StreamBasedSynchronousRemotingTest {

    private class EchoServiceImpl : EchoService {

        override suspend fun echo(message: String): String = message
    }

    private class Pipe(private val bufferSize: Int) {

        private val lock = Mutex()

        private val queue: MutableQueue<Byte> = LinkedQueue<Byte>()

        val source = object : AsyncInputStream {

            override suspend fun close() {
            }

            override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int =
                lock.withLock {
                    for (i in 0..<len) {
                        buffer[offset + i] = queue.dequeueOrNull() ?: return i
                    }

                    len
                }
        }

        val sink = object : AsyncOutputStream {

            override suspend fun close() {
            }

            override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
                lock.withLock {
                    buffer.view(offset, len).forEach {
                        queue.enqueue(it)
                    }
                }
            }
        }
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

            launch(newSingleThreadContext("server")) {
                assertFailsWith(InterruptedIOException::class) {
                    StreamBasedSynchronousRemotingServer(
                        messageCodec,
                        listOf(EchoService.remoteCallDelegator(EchoServiceImpl())),
                        clientToServerPipeSource,
                        serverToClientPipe.sink
                    ).listen()
                }
            }

            val remotingClient = StreamBasedSynchronousRemotingClient(
                messageCodec,
                serverToClientPipe.source,
                clientToServerPipe.sink
            )
            val proxy = EchoService.clientProxy(remotingClient)
            assertEquals("Hello!!!", proxy.echo("Hello!!!"))
            assertEquals("Hello World!", proxy.echo("Hello World!"))
        }
    }
}
