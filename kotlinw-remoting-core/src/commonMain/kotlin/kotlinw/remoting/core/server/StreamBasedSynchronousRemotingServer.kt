package kotlinw.remoting.core.server

import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.codec.BinaryMessageCodecWithMetadataPrefetchSupport
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinx.coroutines.yield
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.serialization.KSerializer
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor
import xyz.kotlinw.remoting.api.internal.RemotingMethodDescriptor

class StreamBasedSynchronousRemotingServer(
    private val messageCodec: BinaryMessageCodecWithMetadataPrefetchSupport,
    remoteCallHandlers: Iterable<RemoteCallHandler>,
    private val source: Source,
    private val sink: Sink
) {
    private val delegators =
        (remoteCallHandlers as Iterable<RemoteCallHandlerImplementor>)
            .associateBy { it.servicePath }

    suspend fun listen(): Nothing {
        while (true) {
            println("Waiting for message...")
            val extractedMetadata = messageCodec.extractMetadata(source)
            val metadata = extractedMetadata.metadata
            check(metadata != null) { "Protocol error: missing metadata." }

            val messageKind = metadata.messageKind ?: TODO()
            check(messageKind is RemotingMessageKind.CallRequest)
            val serviceLocator = messageKind.serviceLocator

            val serviceId = serviceLocator.serviceId
            val delegator = delegators[serviceId]
            check(delegator != null) { "Service not found: $serviceLocator" }

            val methodId = serviceLocator.methodId
            val methodDescriptor = delegator.methodDescriptors[methodId]
            check(methodDescriptor != null) { "Method not found: $serviceLocator" }
            check(methodDescriptor is RemotingMethodDescriptor.SynchronousCall<*, *>)

            val parameter = extractedMetadata.decodePayload(methodDescriptor.parameterSerializer)
            val result = delegator.processCall(methodId, parameter)
            println("called $serviceLocator: $parameter returned $result")

            val rawResult = messageCodec.encodeMessage(
                RemotingMessage(result, null),
                methodDescriptor.resultSerializer as KSerializer<Any?>
            ).byteArrayView

            sink.write(rawResult.toReadOnlyByteArray())

            yield()
        }
    }
}
