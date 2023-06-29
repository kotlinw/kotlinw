package kotlinw.remoting.core.ktor

import arrow.core.continuations.AtomicRef
import arrow.core.continuations.getAndUpdate
import arrow.core.continuations.update
import arrow.core.nonFatalOrThrow
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.collections.set
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class WebSocketConnection(
    private val messageCodec: MessageCodecWithMetadataPrefetchSupport<RawMessage>,
    private val bidirectionalConnection: BidirectionalMessagingConnection
) {
    private class ActiveColdFlowData(val flowManagerCoroutineJob: Job) {

        val suspendedCoroutine = AtomicRef<Continuation<Unit>?>(null)
    }

    private val activeColdFlows: ConcurrentMutableMap<String, ActiveColdFlowData> = ConcurrentHashMap()

    private val supervisorScope = CoroutineScope(SupervisorJob(bidirectionalConnection.coroutineContext.job))

    fun onColdFlowValueCollected(flowId: String) {
        val activeColdFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
        val continuation = checkNotNull(activeColdFlowData.suspendedCoroutine.getAndUpdate { null })
        continuation.resume(Unit)
    }

    fun addActiveColdFlow(flowId: String, flow: Flow<Any?>, flowValueSerializer: KSerializer<Any?>) {
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
                } catch (e: Exception) {
                    throw e.nonFatalOrThrow() // TODO
                } finally {
                    activeColdFlows.remove(flowId)
                }
            }
        )
    }

    suspend fun <T> send(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>) {
        bidirectionalConnection.sendMessage(
            messageCodec.encodeMessage(message, payloadSerializer)
        )
    }

    fun onCollectColdFlow(flowId: String) {
        val coldFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
        coldFlowData.flowManagerCoroutineJob.start()
    }

    suspend fun processIncomingMessage(rawMessage: RawMessage) {
        val metadata = messageCodec.extractMetadata(rawMessage)
        val messageKind = metadata.metadata?.messageKind ?: throw IllegalStateException() // TODO hibaüz.

        when (messageKind) {
            is RemotingMessageKind.CallRequest -> TODO(messageKind.toString())

            is RemotingMessageKind.CallResponse -> TODO(messageKind.toString())

            is RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted -> TODO(messageKind.toString())

            is RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue -> TODO(messageKind.toString())

            is RemotingMessageKind.CollectColdFlow ->
                onCollectColdFlow(messageKind.callId)

            is RemotingMessageKind.ColdFlowValueCollected ->
                onColdFlowValueCollected(messageKind.callId)
        }
    }
}
