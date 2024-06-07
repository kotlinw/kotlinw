package kotlinw.remoting.core

import kotlinw.remoting.core.common.ConversationId
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemotingMessageMetadata(
    val timestamp: Instant? = null,
    val messageKind: RemotingMessageKind? = null
)

@Serializable
data class ServiceLocator(val serviceId: String, val methodId: String) {

    override fun toString() = "$serviceId.$methodId"
}

@Serializable
sealed class RemotingMessageKind {

    abstract val callId: ConversationId // TODO menjen a RemotingMessageMetadata-ba

    @Serializable
    sealed class RequestMessageKind : RemotingMessageKind()

    @Serializable
    sealed class ResponseMessageKind : RemotingMessageKind()

    @Serializable
    sealed class Failed : RemotingMessageKind() {

        @Serializable
        @SerialName("ExceptionThrown")
        data class ExceptionThrown(override val callId: String, val message: String?) : Failed()

        @Serializable
        @SerialName("Cancelled")
        data class Cancelled(override val callId: String, val message: String?) : Failed()
    }

    @Serializable
    @SerialName("Request")
    data class CallRequest(
        override val callId: String,
        val serviceLocator: ServiceLocator
    ) : RequestMessageKind()

    @Serializable
    @SerialName("Response")
    data class CallResponse(override val callId: String) : ResponseMessageKind()

    @Serializable
    @SerialName("CollectColdFlow")
    data class CollectColdFlow(override val callId: String) : RequestMessageKind()

    @Serializable
    sealed class ColdFlowCollectKind : ResponseMessageKind() {

        @Serializable
        @SerialName("ColdFlowValue")
        data class ColdFlowValue(override val callId: String) : ColdFlowCollectKind()

        /**
         * Indicates that the flow completed normally.
         */
        @Serializable
        @SerialName("ColdFlowCompleted")
        data class ColdFlowCompleted(override val callId: String) : ColdFlowCollectKind()
    }

    @Serializable
    @SerialName("ColdFlowValueCollected")
    data class ColdFlowValueCollected(override val callId: String) : RequestMessageKind()
}
