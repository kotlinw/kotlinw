package kotlinw.remoting.core.client

import kotlinw.remoting.api.internal.client.RemotingClientSynchronousCallSupport
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.codec.BinaryMessageCodec
import kotlinw.util.stdlib.write
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import okio.BufferedSink
import okio.BufferedSource
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class StreamBasedSynchronousRemotingClient(
    private val messageCodec: BinaryMessageCodec,
    private val source: BufferedSource,
    private val sink: BufferedSink
) : RemotingClientSynchronousCallSupport {

    private val mutex = Mutex()

    override suspend fun <T : Any, P : Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val parameterMessage = RemotingMessage(
            parameter,
            RemotingMessageMetadata(serviceLocator = ServiceLocator(serviceName, methodName))
        )
        val rawParameterMessage = messageCodec.encodeMessage(parameterMessage, parameterSerializer)

        val resultMessage = mutex.withLock {
            sink.write(rawParameterMessage.byteArrayView)
            sink.flush()

            messageCodec.decodeMessage(source, resultDeserializer)
        }

        return resultMessage.payload
    }
}
