package kotlinw.remoting.core.client

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
import kotlinx.io.Sink
import kotlinx.io.Source
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport

class StreamBasedSynchronousRemotingClient(
    private val messageCodec: BinaryMessageCodecWithMetadataPrefetchSupport,
    private val source: Source,
    private val sink: Sink
) : RemotingClientCallSupport {

    private val mutex = Mutex()

    override suspend fun <P : Any, R> call(
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val callId = Uuid.randomUuid().toString()
        val parameterMessage = RemotingMessage(
            parameter,
            RemotingMessageMetadata(messageKind = RemotingMessageKind.CallRequest(callId, ServiceLocator(serviceId, methodId)))
        )
        val rawParameterMessage = messageCodec.encodeMessage(parameterMessage, parameterSerializer)

        val resultMessage = mutex.withLock {
            sink.write(rawParameterMessage.byteArrayView.toReadOnlyByteArray())
            messageCodec.decodeMessage(source, resultDeserializer)
        }

        return resultMessage.payload
    }
}
