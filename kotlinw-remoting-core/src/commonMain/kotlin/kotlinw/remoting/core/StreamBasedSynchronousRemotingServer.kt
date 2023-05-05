package kotlinw.remoting.core

import kotlinw.remoting.server.core.RemoteCallDelegator
import kotlinw.remoting.server.core.RemotingMethodDescriptor
import kotlinw.util.stdlib.write
import kotlinx.coroutines.yield
import kotlinx.serialization.KSerializer
import okio.BufferedSink
import okio.BufferedSource

class StreamBasedSynchronousRemotingServer(
    private val messageCodec: BinaryMessageCodec,
    remoteCallDelegators: Iterable<RemoteCallDelegator>,
    private val source: BufferedSource,
    private val sink: BufferedSink,
) {
    private val delegators = remoteCallDelegators.associateBy { it.servicePath }

    suspend fun listen(): Nothing {
        while (true) {
            println("Waiting for message...")
            val extractedMetadata = messageCodec.extractMetadata(source)
            val metadata = extractedMetadata.metadata
            check(metadata != null) { "Protocol error: missing metadata." }

            val serviceLocator = metadata.serviceLocator
            check(serviceLocator != null) { "Protocol error: missing service locator. metadata=$metadata" }

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
                methodDescriptor.resultSerializer as KSerializer<Any>
            ).byteArrayView

            sink.write(rawResult)
            sink.flush()

            yield()
        }
    }
}
