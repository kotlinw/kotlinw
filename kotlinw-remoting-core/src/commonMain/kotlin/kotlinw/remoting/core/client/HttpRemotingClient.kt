package kotlinw.remoting.core.client

import arrow.atomic.AtomicBoolean
import arrow.core.continuations.AtomicRef
import arrow.core.nonFatalOrThrow
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.remoting.api.internal.client.RemotingClientDownstreamFlowSupport
import kotlinw.remoting.api.internal.client.RemotingClientSynchronousCallSupport
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.*
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.collection.ConcurrentHashSet
import kotlinw.util.stdlib.concurrent.value
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.KSerializer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class HttpRemotingClient<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpSupportImplementor: SynchronousCallSupport,
    private val remoteServerBaseUrl: Url,
    private val incomingCallDelegators: Map<String, RemoteCallDelegator>
) : RemotingClientSynchronousCallSupport, RemotingClientDownstreamFlowSupport {

    private sealed interface BidirectionalMessagingStatus {

        object NotInitialized : BidirectionalMessagingStatus

        data class NotConnected(val messagingLoopContinuation: Continuation<Unit>) :
            BidirectionalMessagingStatus

        class Connecting : BidirectionalMessagingStatus {
            val coroutinesAwaitingConnection: MutableCollection<Continuation<Unit>> = ConcurrentHashSet()
        }

        data class Connected(val messagingManager: BidirectionalMessagingManager) : BidirectionalMessagingStatus
    }

    private val statusHolder = AtomicRef<BidirectionalMessagingStatus>(BidirectionalMessagingStatus.NotInitialized)

    private val messagingLoopRunningFlag = AtomicBoolean(false)

    private val isReconnectingAutomatically = incomingCallDelegators.isNotEmpty()

    private suspend fun <T> withMessagingManager(block: suspend BidirectionalMessagingManager.() -> T): T {
        var messagingManager: BidirectionalMessagingManager? = null
        while (true) {
            when (val currentStatus = statusHolder.value) {
                is BidirectionalMessagingStatus.Connected -> {
                    messagingManager = currentStatus.messagingManager
                    break
                }

                is BidirectionalMessagingStatus.Connecting -> {
                    suspendCancellableCoroutine<Unit> {
                        currentStatus.coroutinesAwaitingConnection.add(it)
                    }
                }

                is BidirectionalMessagingStatus.NotConnected -> {
                    statusHolder.value = BidirectionalMessagingStatus.Connecting()
                    currentStatus.messagingLoopContinuation.resume(Unit)
                }

                BidirectionalMessagingStatus.NotInitialized -> throw IllegalStateException("Not initialized yet.")
            }
        }

        return messagingManager?.block() ?: throw IllegalStateException()
    }

    suspend fun runMessagingLoop(): Nothing {
        if (messagingLoopRunningFlag.compareAndSet(false, true)) {
            check(httpSupportImplementor is BidirectionalCommunicationImplementor)

            val wsUrl = Url(
                "${
                    remoteServerBaseUrl.value.replace( // TODO
                        "http",
                        "ws"
                    )
                }/remoting/websocket"
            )

            while (true) {
                if (!isReconnectingAutomatically) {
                    suspendCancellableCoroutine {
                        statusHolder.value = BidirectionalMessagingStatus.NotConnected(it)
                    }
                } else {
                    statusHolder.value = BidirectionalMessagingStatus.Connecting()
                }

                try {
                    httpSupportImplementor.runInSession(wsUrl, messageCodec) {
                        val messagingManager = BidirectionalMessagingManagerImpl(
                            this,
                            messageCodec as MessageCodecWithMetadataPrefetchSupport<M>,
                            incomingCallDelegators
                        )

                        val previousStatus = statusHolder.value
                        check(previousStatus is BidirectionalMessagingStatus.Connecting)

                        statusHolder.value = BidirectionalMessagingStatus.Connected(messagingManager)
                        messagingManager.processIncomingMessages()

                        previousStatus.coroutinesAwaitingConnection.forEach {
                            it.resume(Unit)
                        }
                    }
                } catch (e: Exception) {
                    // TODO specifikus exception-ök külön elkapása
                    e.nonFatalOrThrow().printStackTrace() // TODO log
                } finally {
                    withContext(NonCancellable) {
                        val previousStatus = statusHolder.value
                        if (previousStatus is BidirectionalMessagingStatus.Connected) {
                            try {
                                previousStatus.messagingManager.close()
                            } catch (e: Throwable) {
                                e.nonFatalOrThrow().printStackTrace() // TODO log
                            }
                        }
                    }
                }
            }
        } else {
            throw IllegalStateException("Messaging loop is already running.")
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
        return if (statusHolder.value is BidirectionalMessagingStatus.Connected) {
            withMessagingManager {
                call(
                    ServiceLocator(serviceName, methodName),
                    parameter,
                    parameterSerializer,
                    resultDeserializer
                )
            }
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
        withMessagingManager {
            requestColdFlowResult(
                ServiceLocator(serviceId, methodId),
                parameter,
                parameterSerializer,
                flowValueDeserializer,
                callId
            )
        }
}
