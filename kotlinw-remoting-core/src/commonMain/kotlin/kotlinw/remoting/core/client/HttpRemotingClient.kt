package kotlinw.remoting.core.client

import arrow.core.continuations.AtomicRef
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.remoting.api.internal.client.RemotingClientDownstreamFlowSupport
import kotlinw.remoting.api.internal.client.RemotingClientSynchronousCallSupport
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.util.stdlib.concurrent.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

class HttpRemotingClient<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpSupportImplementor: SynchronousCallSupport,
    private val remoteServerBaseUrl: Url
) : RemotingClientSynchronousCallSupport, RemotingClientDownstreamFlowSupport {

    interface BidirectionalCommunicationImplementor {

        suspend fun <M : RawMessage> connect(
            url: Url,
            messageCodecDescriptor: MessageCodecDescriptor
        ): BidirectionalMessagingConnection<M>
    }

    private data class SuspendedCoroutineData<T>(
        val continuation: Continuation<RemotingMessage<T>>,
        val payloadDeserializer: KSerializer<T>
    )

    private val bidirectionalMessagingSupportHolder = AtomicRef<BidirectionalMessagingManager?>(null)

    private val bidirectionalMessagingSupportLock = Mutex()

    private suspend fun ensureConnected(): BidirectionalMessagingManager =
        bidirectionalMessagingSupportLock.withLock {
            bidirectionalMessagingSupportHolder.value ?: run {
                check(httpSupportImplementor is BidirectionalCommunicationImplementor)
                httpSupportImplementor.connect<M>(
                    Url(
                        "${
                            remoteServerBaseUrl.value.replace(
                                "http",
                                "ws"
                            )
                        }/remoting/websocket"
                    ), messageCodec
                )
                    .let { // TODO fix string
                        BidirectionalMessagingManagerImpl(
                            it,
                            messageCodec as MessageCodecWithMetadataPrefetchSupport<M>
                        ).also {
                            bidirectionalMessagingSupportHolder.value = it
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
    ): R {
        val parameterMessage = RemotingMessage(parameter, null) // TODO metadata
        val rawResultMessage =
            messageCodec.encodeMessage(parameterMessage, parameterSerializer)

        val rawResponseMessage = httpSupportImplementor.call(
            buildServiceUrl(serviceName, methodName),
            rawResultMessage,
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

        return resultMessage.payload
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
    ): Flow<F> {
        val connection = ensureConnected()

        val resultFlow = flow {

            connection.sendMessage(
                RemotingMessage(
                    Unit,
                    RemotingMessageMetadata(
                        messageKind = RemotingMessageKind.CollectColdFlow(callId)
                    )// TODO metadata
                ),
                serializer()
            )

            while (true) {
                val message = connection.awaitMessage(callId, flowValueDeserializer)
                when (message.metadata!!.messageKind!!) {
                    is RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue -> emit(message.payload)
                    is RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted -> break
                    else -> throw IllegalStateException()
                }

                connection.sendMessage(
                    RemotingMessage(
                        Unit,
                        RemotingMessageMetadata(
                            messageKind = RemotingMessageKind.ColdFlowValueCollected(callId)
                        )
                    ),
                    serializer()
                )
            }
        }

        val parameterMessage = RemotingMessage(
            parameter,
            RemotingMessageMetadata(
                messageKind = RemotingMessageKind.CallRequest(callId)
            )
        ) // TODO metadata
        val rawResultMessage =
            messageCodec.encodeMessage(parameterMessage, parameterSerializer)

        httpSupportImplementor.call(
            buildServiceUrl(serviceId, methodId),
            rawResultMessage,
            messageCodec
        )

        return resultFlow
    }
}
