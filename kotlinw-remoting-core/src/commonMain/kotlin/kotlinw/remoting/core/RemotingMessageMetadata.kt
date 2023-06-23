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
data class ServiceLocator(val serviceId: String, val methodId: String)

@Serializable
sealed class RemotingMessageKind {

    abstract val callId: String

    @Serializable
    @SerialName("Request")
    data class CallRequest(override val callId: String) : RemotingMessageKind() // TODO : SynchronousCallMessage()

    @Serializable
    @SerialName("Response")
    data class CallResponse(override val callId: String) : RemotingMessageKind() // TODO : SynchronousCallMessage()

    @Serializable
    @SerialName("CollectColdFlow")
    data class CollectColdFlow(override val callId: String) : RemotingMessageKind()

    @Serializable
    sealed class ColdFlowCollectKind : RemotingMessageKind() {

        @Serializable
        @SerialName("ColdFlowValue")
        data class ColdFlowValue(override val callId: String) : ColdFlowCollectKind()

        @Serializable
        @SerialName("ColdFlowCompleted")
        data class ColdFlowCompleted(override val callId: String, val normally: Boolean) :
            ColdFlowCollectKind() // TODO normally/exception/cancelled
    }

    @Serializable
    @SerialName("ColdFlowValueCollected")
    data class ColdFlowValueCollected(override val callId: String) : RemotingMessageKind()
}
