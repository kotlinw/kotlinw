package kotlinw.remoting.core.client

import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncOutputStream
import kotlinw.remoting.api.internal.client.RemotingClientSynchronousCallSupport
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.codec.BinaryMessageCodecWithMetadataPrefetchSupport
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class StreamBasedSynchronousRemotingClient(
    private val messageCodec: BinaryMessageCodecWithMetadataPrefetchSupport,
    private val source: AsyncInputStream,
    private val sink: AsyncOutputStream
) : RemotingClientSynchronousCallSupport {

    private val mutex = Mutex()

    override suspend fun <T : Any, P : Any, R> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val callId = Uuid.randomUuid().toString()
        val parameterMessage = RemotingMessage(
            parameter,
            RemotingMessageMetadata(messageKind = RemotingMessageKind.CallRequest(callId, ServiceLocator(serviceName, methodName)))
        )
        val rawParameterMessage = messageCodec.encodeMessage(parameterMessage, parameterSerializer)

        val resultMessage = mutex.withLock {
            sink.write(rawParameterMessage.byteArrayView.toReadOnlyByteArray())
            messageCodec.decodeMessage(source, resultDeserializer)
        }

        return resultMessage.payload
    }
}
