package kotlinw.remoting.core.common

import arrow.core.continuations.AtomicRef
import arrow.core.continuations.getAndUpdate
import arrow.core.continuations.update
import arrow.core.nonFatalOrThrow
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.util.stdlib.concurrent.value
import kotlinw.util.stdlib.debugName
import kotlinw.uuid.Uuid.Companion.randomUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import xyz.kotlinw.remoting.api.RemoteCallContext
import xyz.kotlinw.remoting.api.RemoteCallContextElement
import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor
import xyz.kotlinw.remoting.api.internal.RemotingMethodDescriptor

interface BidirectionalMessagingManager : CoroutineScope {

    val remoteConnectionId: RemoteConnectionId

    suspend fun <P : Any, F> requestColdFlowResult(
        serviceLocator: ServiceLocator,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        flowValueDeserializer: KSerializer<F>,
        callId: String
    ): Flow<F>

    suspend fun <P : Any, R> call(
        serviceLocator: ServiceLocator,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R

    suspend fun processIncomingMessages() // TODO Nothing return type?

    suspend fun close()
}

class BidirectionalMessagingManagerImpl<M : RawMessage>(
    private val bidirectionalConnection: SingleSessionBidirectionalMessagingConnection,
    private val messageCodec: MessageCodecWithMetadataPrefetchSupport<M>,
    private val remoteCallHandlers: Map<String, RemoteCallHandlerImplementor<*>>
) : BidirectionalMessagingManager, CoroutineScope by bidirectionalConnection {

    private val logger =
        PlatformLogging.getLogger(this::class.debugName /* + "/" + bidirectionalConnection.peerId + "/" + bidirectionalConnection.sessionId */)

    private class InitiatedConversationData(val callId: ConversationId) {

        val suspendedCoroutineData = AtomicRef<SuspendedCoroutineData<*>?>(null)
    }

    private data class SuspendedCoroutineData<T>(
        val continuation: Continuation<RemotingMessage<T>>,
        val payloadDeserializer: KSerializer<T>
    )

    // TODO disconnect vagy valamilyen timeout esetén cancel-elni és eltávolítani ezeket
    private val initiatedConversations: ConcurrentMutableMap<ConversationId, InitiatedConversationData> =
        ConcurrentHashMap()

    private class ActiveColdFlowData(val flowManagerCoroutineJob: Job) {

        val suspendedCoroutine = AtomicRef<Continuation<Unit>?>(null)
    }

    private val activeColdFlows: ConcurrentMutableMap<String, ActiveColdFlowData> = ConcurrentHashMap()

    private val flowManagerScope =
        CoroutineScope(bidirectionalConnection.coroutineContext + SupervisorJob(bidirectionalConnection.coroutineContext.job))

    override val remoteConnectionId: RemoteConnectionId get() = bidirectionalConnection.remoteConnectionId

    override suspend fun processIncomingMessages() {
        logger.debug { "Starting message processing: " / bidirectionalConnection }

        val remoteConnectionId = bidirectionalConnection.remoteConnectionId

        bidirectionalConnection.incomingRawMessages().collect { rawMessage ->
            logger.trace { "Received raw message: " / rawMessage }

            val extractedMetadata = messageCodec.extractMetadata(rawMessage as M)
            val metadata = checkNotNull(extractedMetadata.metadata)
            val messageKind = checkNotNull(metadata.messageKind)
            val callId = messageKind.callId

            when (messageKind) {
                is RemotingMessageKind.CallRequest -> {
                    val serviceId = messageKind.serviceLocator.serviceId
                    val targetServiceDescriptor =
                        remoteCallHandlers[serviceId]
                            ?: throw IllegalStateException("Unexpected remote service ID: $serviceId")
                    val methodId = messageKind.serviceLocator.methodId
                    val targetMethodDescriptor = targetServiceDescriptor.methodDescriptors[methodId]
                        ?: throw IllegalStateException("Unexpected remote method ID: $methodId")

                    logger.debug { "Incoming RPC call: " / serviceId / "." / methodId }

                    when (targetMethodDescriptor) {
                        is RemotingMethodDescriptor.DownstreamColdFlow<*, *> -> {
                            val resultFlow =
                                withContext(RemoteCallContextElement(RemoteCallContext(remoteConnectionId))) {
                                    targetServiceDescriptor.processCall(
                                        methodId,
                                        extractedMetadata.decodePayload(targetMethodDescriptor.parameterSerializer)
                                    ) as Flow<Any?>
                                }

                            addActiveColdFlow(
                                callId,
                                resultFlow,
                                targetMethodDescriptor.flowValueSerializer as KSerializer<Any?>
                            )

                            sendReplyMessage(
                                RemotingMessage(
                                    Unit,
                                    RemotingMessageMetadata(messageKind = RemotingMessageKind.CallResponse(callId))
                                ),
                                serializer()
                            )
                        }

                        is RemotingMethodDescriptor.SynchronousCall<*, *> -> {
                            val result =
                                withContext(RemoteCallContextElement(RemoteCallContext(remoteConnectionId))) {
                                    targetServiceDescriptor.processCall(
                                        methodId,
                                        extractedMetadata.decodePayload(targetMethodDescriptor.parameterSerializer)
                                    )
                                }

                            sendReplyMessage(
                                RemotingMessage(
                                    result,
                                    RemotingMessageMetadata(messageKind = RemotingMessageKind.CallResponse(callId))
                                ),
                                targetMethodDescriptor.resultSerializer as KSerializer<Any?>
                            )
                        }
                    }
                }

                is RemotingMessageKind.ColdFlowCollectKind, is RemotingMessageKind.CallResponse -> {
                    val conversationData = initiatedConversations[callId]
                        ?: throw IllegalStateException("The requested conversation does not exist: peerId=${remoteConnectionId.peerId}, callMetadata=$metadata")

                    val suspendedCoroutineData =
                        conversationData.suspendedCoroutineData.value ?: throw IllegalStateException()

                    when (messageKind) {
                        is RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted -> {
                            val message =
                                extractedMetadata.decodeMessage(serializer<Unit>()) //TODO unit helyett null kellene legyen
                            suspendedCoroutineData.continuation.resume(message as RemotingMessage<Nothing>)
                        }

                        is RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue -> {
                            val message =
                                extractedMetadata.decodeMessage(suspendedCoroutineData.payloadDeserializer)
                            suspendedCoroutineData.continuation.resume(message as RemotingMessage<Nothing>)
                        }

                        is RemotingMessageKind.CallResponse -> {
                            val message =
                                extractedMetadata.decodeMessage(suspendedCoroutineData.payloadDeserializer)
                            suspendedCoroutineData.continuation.resume(message as RemotingMessage<Nothing>)
                        }

                        else -> throw IllegalStateException()
                    }
                }

                is RemotingMessageKind.ColdFlowValueCollected -> onColdFlowValueCollected(callId)

                is RemotingMessageKind.CollectColdFlow -> {
                    onCollectColdFlow(callId)
                }
            }
        }

        logger.debug { "Incoming message stream ended: " / bidirectionalConnection }
    }

    private suspend fun <T> sendReplyMessage(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>) {
        logger.trace { "Sending reply message: " / message }
        bidirectionalConnection.sendRawMessage(messageCodec.encodeMessage(message, payloadSerializer))
        logger.trace { "Reply message has been sent." }
    }

    private suspend fun <P : Any, R> sendMessageAndAwaitReply(
        callId: String,
        requestMessage: RemotingMessage<P>,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): RemotingMessage<R> {
        return sendMessageAndAwaitReply(
            initiatedConversations[callId] ?: throw IllegalStateException(),
            requestMessage,
            parameterSerializer,
            resultDeserializer
        )
    }

    private suspend fun <P : Any, R> sendMessageAndAwaitReply(
        conversationData: InitiatedConversationData,
        requestMessage: RemotingMessage<P>,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): RemotingMessage<R> {
        logger.trace { "Sending message: " / requestMessage }
        bidirectionalConnection.sendRawMessage(messageCodec.encodeMessage(requestMessage, parameterSerializer))
        logger.trace { "Message has been sent." }

        return suspendCancellableCoroutine {
            logger.trace { "Suspending coroutine until incoming conversation message: " / conversationData.callId }
            conversationData.suspendedCoroutineData.value = SuspendedCoroutineData(it, resultDeserializer)

            it.invokeOnCancellation {
                endConversation(conversationData.callId)
            }
        }
    }

    override suspend fun <P : Any, R> call(
        serviceLocator: ServiceLocator,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val callId = randomUuid().toString()
        logger.debug { "Calling: " / serviceLocator / parameter }

        val requestMessage = RemotingMessage(
            parameter,
            RemotingMessageMetadata(
                messageKind = RemotingMessageKind.CallRequest(callId, serviceLocator)
            )
        ) // TODO metadata

        val responseMessage =
            sendMessageAndAwaitReply(
                initiateConversation(callId),
                requestMessage,
                parameterSerializer,
                resultDeserializer
            )

        endConversation(callId) // TODO try-finally?

        logger.trace { "Response message received: " / responseMessage }
        return responseMessage.payload
    }

    private fun initiateConversation(callId: ConversationId): InitiatedConversationData {
        logger.trace { "Initiating conversation: " / callId }
        val initiatedConversationData = InitiatedConversationData(callId)
        initiatedConversations[callId] = initiatedConversationData
        return initiatedConversationData
    }

    private fun endConversation(callId: ConversationId) {
        logger.trace { "Ending conversation: " / callId }
        initiatedConversations.remove(callId)
    }

    override suspend fun <P : Any, F> requestColdFlowResult(
        serviceLocator: ServiceLocator,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        flowValueDeserializer: KSerializer<F>,
        callId: String
    ): Flow<F> {
        val conversationData = initiateConversation(callId)

        val resultFlow = flow {
            val firstReplyMessage = sendMessageAndAwaitReply(
                callId,
                RemotingMessage(
                    Unit,
                    RemotingMessageMetadata(
                        messageKind = RemotingMessageKind.CollectColdFlow(callId)
                    ) // TODO metadata
                ),
                serializer<Unit>(),
                flowValueDeserializer
            )

            when (firstReplyMessage.metadata!!.messageKind!!) {
                is RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue -> emit(firstReplyMessage.payload)
                is RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted -> {
                    removeColdFlow(callId)
                    endConversation(callId)
                    return@flow
                }

                else -> throw IllegalStateException()
            }

            while (true) {
                val currentReplyMessage = sendMessageAndAwaitReply(
                    callId,
                    RemotingMessage(
                        Unit,
                        RemotingMessageMetadata(
                            messageKind = RemotingMessageKind.ColdFlowValueCollected(callId)
                        ) // TODO metadata
                    ),
                    serializer<Unit>(),
                    flowValueDeserializer
                )

                when (currentReplyMessage.metadata!!.messageKind!!) {
                    is RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue -> emit(currentReplyMessage.payload)
                    is RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted -> {
                        removeColdFlow(callId)
                        endConversation(callId)
                        return@flow
                    }

                    else -> throw IllegalStateException()
                }
            }
        }

        val requestMessage = RemotingMessage(
            parameter,
            RemotingMessageMetadata(
                messageKind = RemotingMessageKind.CallRequest(callId, serviceLocator)
            )
        ) // TODO metadata

        sendMessageAndAwaitReply(conversationData, requestMessage, parameterSerializer, serializer<Unit>())

        return resultFlow
    }

    private fun onColdFlowValueCollected(flowId: String) {
        val activeColdFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
        val continuation = checkNotNull(activeColdFlowData.suspendedCoroutine.getAndUpdate { null })
        continuation.resume(Unit)
    }

    private fun removeColdFlow(flowId: String) {
        activeColdFlows.remove(flowId)
    }

    private fun addActiveColdFlow(flowId: String, flow: Flow<Any?>, flowValueSerializer: KSerializer<Any?>) {
        activeColdFlows[flowId] = ActiveColdFlowData(
            flowManagerScope.launch(start = CoroutineStart.LAZY) {
                try {
                    flow.collect {
                        send(
                            RemotingMessage(
                                it,
                                RemotingMessageMetadata(
                                    messageKind = RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue(flowId)
                                )
                            ),
                            flowValueSerializer
                        )

                        val currentColdFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
                        suspendCancellableCoroutine { continuation ->
                            currentColdFlowData.suspendedCoroutine.update {
                                check(it == null)
                                continuation
                            }

                            continuation.invokeOnCancellation {
                                removeColdFlow(flowId)
                            }
                        }
                    }

                    send(
                        RemotingMessage(
                            Unit, // TODO null
                            RemotingMessageMetadata(
                                messageKind = RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted(flowId, true)
                            )
                        ),
                        serializer<Unit>()
                    )
                } catch (e: Throwable) {
                    logger.error(e.nonFatalOrThrow()) { } // TODO
                } finally {
                    activeColdFlows.remove(flowId)
                }
            }
        )
    }

    private suspend fun <T> send(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>) {
        bidirectionalConnection.sendRawMessage(
            messageCodec.encodeMessage(message, payloadSerializer)
        )
    }

    private fun onCollectColdFlow(flowId: String) {
        val coldFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
        coldFlowData.flowManagerCoroutineJob.start()
    }

    override suspend fun close() {
        // TODO kellene valami lock, hogy close() közben ne lehessen más metódusokat hívni

        try {
            flowManagerScope.cancel()
        } catch (e: Throwable) {
            logger.warning(e.nonFatalOrThrow()) { "Failed to cancel supervisor scope of messaging manager." }
        }

        initiatedConversations.values.forEach {
            try {
                it.suspendedCoroutineData.value?.continuation?.resumeWithException(MessagingChannelDisconnectedException())
            } catch (e: Throwable) {
                logger.warning(e.nonFatalOrThrow()) { "Failed to resume coroutine with exception." }
            }
        }
        initiatedConversations.clear()
    }
}
