package kotlinw.remoting.core

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemotingMessageMetadata(
    val timestamp: Instant? = null,
    val serviceLocator: ServiceLocator? = null,
    val messageKind: RemotingMessageKind? = null
)

@Serializable
sealed class RemotingMessageKind {

    interface HasCallId {

        val callId: String
    }

    @Serializable
    @SerialName("Request")
    data class CallRequest(override val callId: String) : RemotingMessageKind(),
        HasCallId // TODO : SynchronousCallMessage()

    @Serializable
    @SerialName("Response")
    data class CallResponse(override val callId: String) : RemotingMessageKind(),
        HasCallId // TODO : SynchronousCallMessage()

    @Serializable
    @SerialName("SharedFlowValue")
    object SharedFlowValue : RemotingMessageKind() // TODO : SharedFlowMessage() + cancelled

    @Serializable
    @SerialName("CollectColdFlow")
    data class CollectColdFlow(override val callId: String) : RemotingMessageKind(), HasCallId

    @Serializable
    sealed class ColdFlowCollectKind: RemotingMessageKind() {

        @Serializable
        @SerialName("ColdFlowValue")
        data class ColdFlowValue(override val callId: String) : ColdFlowCollectKind(), HasCallId

        @Serializable
        @SerialName("ColdFlowCompleted")
        data class ColdFlowCompleted(override val callId: String, val normally: Boolean) : ColdFlowCollectKind(),
            HasCallId // TODO normally/exception/cancelled
    }

    @Serializable
    @SerialName("ColdFlowValueCollected")
    data class ColdFlowValueCollected(override val callId: String) : RemotingMessageKind(), HasCallId

}
