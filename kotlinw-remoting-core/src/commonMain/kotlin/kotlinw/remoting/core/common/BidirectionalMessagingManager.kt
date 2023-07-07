package kotlinw.remoting.core.common

import arrow.core.continuations.AtomicRef
import arrow.core.continuations.getAndUpdate
import arrow.core.continuations.update
import arrow.core.nonFatalOrThrow
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor
import kotlinw.remoting.core.*
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.util.stdlib.concurrent.value
import kotlinw.uuid.Uuid.Companion.randomUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingSessionId

interface BidirectionalMessagingManager : CoroutineScope {

    val remotePeerId: MessagingPeerId

    val sessionId: MessagingSessionId

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
    private val bidirectionalConnection: BidirectionalMessagingConnection,
    private val messageCodec: MessageCodecWithMetadataPrefetchSupport<M>,
    private val remoteCallDelegators: Map<String, RemoteCallDelegator>,
) : BidirectionalMessagingManager, CoroutineScope by bidirectionalConnection {

    private val logger =
        PlatformLogging.getLogger(this::class.qualifiedName!! + "/" + bidirectionalConnection.peerId + "/" + bidirectionalConnection.sessionId)

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

    private val supervisorScope = CoroutineScope(SupervisorJob(bidirectionalConnection.coroutineContext.job))


    override val remotePeerId get() = bidirectionalConnection.peerId

    override val sessionId get() = bidirectionalConnection.sessionId

    override suspend fun processIncomingMessages() {
        logger.debug { "Starting message processing: " / bidirectionalConnection }
        bidirectionalConnection.incomingRawMessages().collect { rawMessage ->
            logger.trace { "Received raw message: " / rawMessage }

            val extractedMetadata = messageCodec.extractMetadata(rawMessage as M)
            val metadata = checkNotNull(extractedMetadata.metadata)
            val messageKind = checkNotNull(metadata.messageKind)
            val callId = messageKind.callId

            when (messageKind) {
                is RemotingMessageKind.CallRequest -> {
                    val targetServiceDescriptor =
                        remoteCallDelegators[messageKind.serviceLocator.serviceId] ?: TODO()
                    val methodId = messageKind.serviceLocator.methodId
                    val targetMethodDescriptor = targetServiceDescriptor.methodDescriptors[methodId] ?: TODO()

                    when (targetMethodDescriptor) {
                        is RemotingMethodDescriptor.DownstreamColdFlow<*, *> -> {
                            val resultFlow = targetServiceDescriptor.processCall(
                                methodId,
                                extractedMetadata.decodePayload(targetMethodDescriptor.parameterSerializer)
                            ) as Flow<Any?>

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
                            val result = targetServiceDescriptor.processCall(
                                methodId,
                                extractedMetadata.decodePayload(targetMethodDescriptor.parameterSerializer)
                            )
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
                        ?: throw IllegalStateException("The requested conversation does not exist: peerId=${bidirectionalConnection.peerId}, callMetadata=$metadata")

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
        logger.trace { "Calling: " / serviceLocator / parameter }

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
            supervisorScope.launch(start = CoroutineStart.LAZY) {
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
            supervisorScope.cancel()
        } catch (e: Throwable) {
            logger.warning(e.nonFatalOrThrow()) { "Failed to cancel supervisor scope of messaging manager." }
        }

        try {
            bidirectionalConnection.close()
        } catch (e: Throwable) {
            logger.warning(e.nonFatalOrThrow()) { "Failed to close bidirectional connection." }
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
