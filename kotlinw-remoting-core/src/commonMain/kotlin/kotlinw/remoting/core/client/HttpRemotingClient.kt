package kotlinw.remoting.core.client

import arrow.core.continuations.AtomicRef
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.remoting.api.internal.client.RemotingClientDownstreamFlowSupport
import kotlinw.remoting.api.internal.client.RemotingClientSynchronousCallSupport
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.concurrent.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer

class HttpRemotingClient<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpSupportImplementor: SynchronousCallSupport,
    private val remoteServerBaseUrl: Url,
    private val remoteCallDelegators: Map<String, RemoteCallDelegator>
) : RemotingClientSynchronousCallSupport, RemotingClientDownstreamFlowSupport {

    interface BidirectionalCommunicationImplementor {

        suspend fun connect(
            url: Url,
            messageCodecDescriptor: MessageCodecDescriptor
        ): BidirectionalMessagingConnection
    }

    private val bidirectionalMessagingSupportHolder = AtomicRef<BidirectionalMessagingManager?>(null)

    private val bidirectionalMessagingSupportLock = Mutex()

    private val isConnected get() = bidirectionalMessagingSupportHolder.value != null

    private suspend fun ensureConnected(): BidirectionalMessagingManager =
        bidirectionalMessagingSupportLock.withLock {
            bidirectionalMessagingSupportHolder.value ?: run {
                check(httpSupportImplementor is BidirectionalCommunicationImplementor)
                httpSupportImplementor.connect(
                    Url(
                        "${
                            remoteServerBaseUrl.value.replace( // TODO
                                "http",
                                "ws"
                            )
                        }/remoting/websocket"
                    ), messageCodec
                )
                    .let {// TODO fix string
                        BidirectionalMessagingManagerImpl(
                            it,
                            messageCodec as MessageCodecWithMetadataPrefetchSupport<M>,
                            remoteCallDelegators
                        ).also { messagingManager ->
                            bidirectionalMessagingSupportHolder.value = messagingManager

                            messagingManager.launch(start = CoroutineStart.UNDISPATCHED) {
                                messagingManager.processMessages()
                            }
                        }
                    }
            }
        }

    private fun buildServiceUrl(serviceName: String, methodName: String): String =
        "$remoteServerBaseUrl/remoting/call/$serviceName/$methodName" // TODO

    override suspend fun <T : Any, P : Any, R> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R =
        if (isConnected) {
            ensureConnected().call(
                ServiceLocator(serviceName, methodName),
                parameter,
                parameterSerializer,
                resultDeserializer
            )
        } else {
            val requestMessage = RemotingMessage(parameter, null) // TODO metadata
            val rawRequestMessage =
                messageCodec.encodeMessage(requestMessage, parameterSerializer)
            val rawResponseMessage =
                httpSupportImplementor.call(
                    buildServiceUrl(serviceName, methodName),
                    rawRequestMessage,
                    messageCodec
                )

            val resultMessage =
                try {
                    messageCodec.decodeMessage(rawResponseMessage, resultDeserializer)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Failed to decode response message of RPC method $serviceName.$methodName: $rawResponseMessage",
                        e
                    )
                }

            // TODO metadata

            resultMessage.payload
        }

    override suspend fun <T : Any, P : Any, F> requestDownstreamColdFlow(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<Flow<F>>,
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        flowValueDeserializer: KSerializer<F>,
        callId: String
    ): Flow<F> =
        ensureConnected().requestColdFlowResult(
            ServiceLocator(serviceId, methodId),
            parameter,
            parameterSerializer,
            flowValueDeserializer,
            callId
        )
}
