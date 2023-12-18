package kotlinw.remoting.core.client

import arrow.atomic.AtomicBoolean
import arrow.core.continuations.AtomicRef
import arrow.core.nonFatalOrThrow
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import xyz.kotlinw.remoting.api.internal.RemotingClientFlowSupport
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler
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
import kotlinx.serialization.KSerializer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport

class HttpRemotingClient<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpSupportImplementor: SynchronousCallSupport,
    private val peerRegistry: MutableRemotePeerRegistry,
    private val remoteServerBaseUrl: Url,
    private val incomingCallDelegators: Map<String, RemoteCallHandler>
) : RemotingClientCallSupport, RemotingClientFlowSupport {

    private val logger = PlatformLogging.getLogger()

    private sealed interface BidirectionalMessagingStatus {

        data object NotInitialized : BidirectionalMessagingStatus

        data class NotConnected(val messagingLoopContinuation: Continuation<Unit>) :
            BidirectionalMessagingStatus

        class Connecting : BidirectionalMessagingStatus {
            val coroutinesAwaitingConnection: MutableCollection<Continuation<Unit>> = ConcurrentHashSet()
        }

        data class Connected(val messagingManager: BidirectionalMessagingManager) : BidirectionalMessagingStatus
    }

    private val statusHolder = AtomicRef<BidirectionalMessagingStatus>(BidirectionalMessagingStatus.NotInitialized)

    private val messagingLoopRunningFlag = AtomicBoolean(false)

    private val isReconnectingAutomatically = incomingCallDelegators.isNotEmpty() // TODO add ability to force its value

    private suspend fun <T> withMessagingManager(block: suspend BidirectionalMessagingManager.() -> T): T {
        var messagingManager: BidirectionalMessagingManager? = null
        while (true) {
            when (val currentStatus = statusHolder.value) {
                is BidirectionalMessagingStatus.Connected -> {
                    messagingManager = currentStatus.messagingManager
                    break
                }

                is BidirectionalMessagingStatus.Connecting -> {
                    suspendCancellableCoroutine {
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
                }/remoting/websocket" // TODO fix string
            )

            while (true) {
                if (!isReconnectingAutomatically) {
                    suspendCancellableCoroutine {
                        statusHolder.value = BidirectionalMessagingStatus.NotConnected(it)
                    }
                } else {
                    statusHolder.value = BidirectionalMessagingStatus.Connecting()
                }

                var connectionIdForClosing: RemoteConnectionId? = null
                try {
                    httpSupportImplementor.runInSession(wsUrl, messageCodec) {
                        val connectionId = RemoteConnectionId(peerId, sessionId)
                        connectionIdForClosing = connectionId

                        val messagingManager = BidirectionalMessagingManagerImpl(
                            this,
                            messageCodec as MessageCodecWithMetadataPrefetchSupport<M>,
                            incomingCallDelegators as Map<String, RemoteCallHandlerImplementor>
                        )

                        val previousStatus = statusHolder.value
                        check(previousStatus is BidirectionalMessagingStatus.Connecting)

                        statusHolder.value = BidirectionalMessagingStatus.Connected(messagingManager)

                        previousStatus.coroutinesAwaitingConnection.forEach {
                            it.resume(Unit)
                        }

                        peerRegistry.addConnection(connectionId, RemoteConnectionData(messagingManager))
                        messagingManager.processIncomingMessages()
                    }
                } catch (e: Throwable) {
                    // TODO specifikus exception-ök külön elkapása runInSession()-ben
                    logger.error(e.nonFatalOrThrow()) { "Connection failed." }
                } finally {
                    if (connectionIdForClosing != null) {
                        peerRegistry.removeConnection(connectionIdForClosing!!)
                        connectionIdForClosing = null
                    }

                    withContext(NonCancellable) {
                        val previousStatus = statusHolder.value
                        if (previousStatus is BidirectionalMessagingStatus.Connected) {
                            try {
                                previousStatus.messagingManager.close()
                            } catch (e: Throwable) {
                                logger.debug(e.nonFatalOrThrow()) { "close() failed." }
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
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        // TODO nem szabad, hogy ez itt dőljön el, mert a RemotingConfiguration egyértelműen meghatározza, hogy milyen provider-rel kell végezni a kommunikációt
        return if (statusHolder.value is BidirectionalMessagingStatus.Connected) {
            withMessagingManager {
                call(
                    ServiceLocator(serviceId, methodId),
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
                    buildServiceUrl(serviceId, methodId),
                    rawRequestMessage,
                    messageCodec
                )

            val resultMessage =
                try {
                    messageCodec.decodeMessage(rawResponseMessage, resultDeserializer)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Failed to decode response message of RPC method $serviceId.$methodId: $rawResponseMessage",
                        e
                    )
                }

            // TODO metadata

            resultMessage.payload
        }
    }

    override suspend fun <T : Any, P : Any, F> requestIncomingColdFlow(
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
