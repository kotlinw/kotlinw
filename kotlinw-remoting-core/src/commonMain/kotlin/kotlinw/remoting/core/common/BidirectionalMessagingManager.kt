package kotlinw.remoting.core.common

import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

interface BidirectionalMessagingManager {

    suspend fun <T> awaitMessage(callId: String, payloadDeserializer: KSerializer<T>): RemotingMessage<T>

    suspend fun <T> sendMessage(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>)
}

class BidirectionalMessagingManagerImpl<M : RawMessage>(
    private val bidirectionalConnection: BidirectionalRawMessagingConnection,
    private val messageCodec: MessageCodecWithMetadataPrefetchSupport<M>
) : BidirectionalMessagingManager {

    private data class SuspendedCoroutineData<T>(
        val continuation: Continuation<RemotingMessage<T>>,
        val payloadDeserializer: KSerializer<T>
    )

    // TODO disconnect esetén cancel-elni és eltávolítani ezeket
    private val suspendedCoroutines: ConcurrentMutableMap<String, SuspendedCoroutineData<*>> = ConcurrentHashMap()

    init {
        bidirectionalConnection.launch {
            bidirectionalConnection.incomingRawMessages().collect { rawMessage ->
                val extractedMetadata = messageCodec.extractMetadata(rawMessage as M)
                val metadata = checkNotNull(extractedMetadata.metadata)
                val messageKind = checkNotNull(metadata.messageKind)

                when (messageKind) {
                    is RemotingMessageKind.CallRequest -> TODO()

                    else -> {
                        val suspendedCoroutineData = suspendedCoroutines.remove(messageKind.callId)
                            ?: throw IllegalStateException() // TODO hibaüz.

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

                            is RemotingMessageKind.CallResponse -> TODO()

                            is RemotingMessageKind.ColdFlowValueCollected -> TODO()

                            is RemotingMessageKind.CollectColdFlow -> TODO()

                            else -> throw IllegalStateException()
                        }
                    }
                }
            }
        }
    }

    override suspend fun <T> awaitMessage(callId: String, payloadDeserializer: KSerializer<T>): RemotingMessage<T> =
        suspendCancellableCoroutine {
            suspendedCoroutines[callId] = SuspendedCoroutineData(it, payloadDeserializer)
        }

    override suspend fun <T> sendMessage(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>) {
        bidirectionalConnection.sendRawMessage(messageCodec.encodeMessage(message, payloadSerializer))
    }
}
