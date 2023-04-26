package kotlinw.remoting.core

import kotlinw.remoting.client.core.RemotingClientImplementor
import kotlinw.remoting.server.core.RemoteCallDelegator
import kotlinw.util.stdlib.write
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.buffer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class StreamBasedSynchronousRemotingClient(
    private val messageCodec: BinaryMessageCodec,
    private val source: BufferedSource,
    private val sink: BufferedSink
) : RemotingClientImplementor {

    private val mutex = Mutex()

    private val isBinaryCodec = messageCodec.isBinary

    override suspend fun <T : Any, P : Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<*>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val parameterMessage = RemotingMessage(parameter, null) // TODO metadata
        val rawParameterMessage = messageCodec.encodeMessage(parameterMessage, parameterSerializer)

        val resultMessage = mutex.withLock {
            sink.write(rawParameterMessage.byteArrayView)
            messageCodec.decodeMessage(source, resultDeserializer)
        }

        return resultMessage.payload
    }
}
