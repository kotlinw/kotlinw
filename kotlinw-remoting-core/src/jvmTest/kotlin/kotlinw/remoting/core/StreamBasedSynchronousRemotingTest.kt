// TODO move to commonTest
package kotlinw.remoting.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinw.collection.LinkedQueue
import kotlinw.collection.MutableQueue
import kotlinw.remoting.core.client.StreamBasedSynchronousRemotingClient
import kotlinw.remoting.core.codec.BinaryMessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.codec.KotlinxSerializationTextMessageCodec
import kotlinw.remoting.core.codec.asBinaryMessageCodec
import kotlinw.remoting.core.server.StreamBasedSynchronousRemotingServer
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import java.io.InterruptedIOException
import java.util.concurrent.locks.ReentrantLock

// TODO freezes
class StreamBasedSynchronousRemotingTest {

    private class EchoServiceImpl : EchoService {

        override suspend fun echo(message: String): String = message
    }

    private class Pipe {

        private val lock = ReentrantLock()

        private val queue: MutableQueue<Byte> = LinkedQueue()

        val source = object : RawSource {

            override fun close() {
            }

            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long =
                lock.withLock {
                    for (i in 0..<byteCount) {
                        val b = queue.dequeueOrNull() ?: return i
                        sink.writeByte(b)
                    }

                    byteCount
                }
        }.buffered()

        val sink = object : RawSink {

            override fun close() {
            }

            override fun flush() {
            }

            override fun write(source: Buffer, byteCount: Long) {
                lock.withLock {
                    val bytes = source.readByteArray(byteCount.toInt())
                    bytes.forEach {
                        queue.enqueue(it)
                    }
                }
            }
        }.buffered()
    }

    @Test
    fun testEcho() {
        runBlocking {
            val messageCodec = BinaryMessageCodecWithMetadataPrefetchSupport(
                KotlinxSerializationTextMessageCodec(Json, "application/json").asBinaryMessageCodec()
            )

            val serverToClientPipe = Pipe()
            val clientToServerPipe = Pipe()

            val clientToServerPipeSource = clientToServerPipe.source.buffered()

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
