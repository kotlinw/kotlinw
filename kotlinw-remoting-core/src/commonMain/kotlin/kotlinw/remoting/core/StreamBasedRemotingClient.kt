package kotlinw.remoting.core

import kotlinw.remoting.client.core.RemotingClientImplementor
import kotlinw.remoting.server.core.RawMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import okio.Sink
import okio.Source
import okio.buffer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class StreamBasedSynchronousRemotingClient(
    private val messageCodec: MessageCodecImplementor,
    source: Source,
    sink: Sink
) : RemotingClientImplementor {

    private val mutex = Mutex()

    private val bufferedSource = source.buffer()

    private val bufferedSink = sink.buffer()

    private val isTextCodec = messageCodec.descriptor.isText

    override suspend fun <T : Any, P : Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<*>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val rawRequestMessage = messageCodec.encodeMessage(parameter, parameterSerializer)
        val bytes = rawRequestMessage.toByteArray()

        val rawResponseMessage =
            mutex.withLock {
                bufferedSink.writeInt(bytes.size)
                bufferedSink.write(bytes)
                bufferedSink.flush()

                val rawResponseMessageSize = bufferedSource.readInt()
                bufferedSource.readByteArray(rawResponseMessageSize.toLong())
            }.let {
                if (isTextCodec) RawMessage.Text.of(it) else RawMessage.Binary(it)
            }

        return messageCodec.decodeMessage(rawResponseMessage, resultDeserializer)
    }
}
