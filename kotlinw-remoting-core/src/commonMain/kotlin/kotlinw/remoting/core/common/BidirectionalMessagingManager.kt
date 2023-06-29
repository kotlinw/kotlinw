package kotlinw.remoting.core.common

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
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

interface BidirectionalMessagingManager {

    suspend fun <T> awaitMessage(callId: String, payloadDeserializer: KSerializer<T>): RemotingMessage<T>

    suspend fun <T> sendMessage(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>)
}


class BidirectionalMessagingManagerImpl<M : RawMessage>(
    private val bidirectionalConnection: BidirectionalMessagingConnection<M>,
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
                val metadataHolder = messageCodec.extractMetadata(rawMessage)
                val metadata = checkNotNull(metadataHolder.metadata)
                val messageKind = checkNotNull(metadata.messageKind)

                suspendedCoroutines.remove(messageKind.callId)?.also {
                    when (messageKind) {
                        is RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted -> {
                            val message =
                                metadataHolder.decodeMessage(serializer<Unit>()) //TODO unit helyett null kellene legyen
                            it.continuation.resume(message as RemotingMessage<Nothing>)
                        }

                        is RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue -> {
                            val message = metadataHolder.decodeMessage(it.payloadDeserializer)
                            it.continuation.resume(message as RemotingMessage<Nothing>)
                        }

                        is RemotingMessageKind.CallRequest -> TODO()

                        is RemotingMessageKind.CallResponse -> TODO()

                        is RemotingMessageKind.ColdFlowValueCollected -> TODO()

                        is RemotingMessageKind.CollectColdFlow -> TODO()
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
        bidirectionalConnection.sendMessage(messageCodec.encodeMessage(message, payloadSerializer))
    }
}
