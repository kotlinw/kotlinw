package kotlinw.remoting.core.client

import arrow.atomic.AtomicBoolean
import arrow.core.nonFatalOrThrow
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.Connected
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.Connecting
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.NotConnected
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.NotInitialized
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalCommunicationImplementor
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.common.DelegatingRemotingClient
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.RemoteConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.collection.ConcurrentHashSet
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import xyz.kotlinw.remoting.api.PersistentRemotingClient
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport
import xyz.kotlinw.remoting.api.internal.RemotingClientFlowSupport

class WebSocketRemotingClientImpl<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpSupportImplementor: SynchronousCallSupport,
    private val peerRegistry: MutableRemotePeerRegistry,
    private val remoteServerBaseUrl: Url,
    private val incomingCallDelegators: Set<RemoteCallHandler<*>>,
    loggerFactory: LoggerFactory,
    private val reconnectAutomatically: Boolean = true
) : RemotingClientCallSupport, RemotingClientFlowSupport, PersistentRemotingClient {

    private val logger = loggerFactory.getLogger()

    private sealed interface BidirectionalMessagingStatus {

        data object NotInitialized : BidirectionalMessagingStatus

        data class NotConnected(val messagingLoopContinuation: Continuation<Unit>) :
            BidirectionalMessagingStatus

        class Connecting : BidirectionalMessagingStatus {
            val coroutinesAwaitingConnection: MutableCollection<Continuation<Unit>> = ConcurrentHashSet()
        }

        data class Connected(val messagingManager: BidirectionalMessagingManager) : BidirectionalMessagingStatus
    }

    private var status: BidirectionalMessagingStatus by atomic(NotInitialized)

    private val messagingLoopRunningFlag = AtomicBoolean(false)

    private suspend fun <T> withMessagingManager(block: suspend BidirectionalMessagingManager.() -> T): T {
        val messagingManager: BidirectionalMessagingManager?
        while (true) {
            when (val currentStatus = status) {
                is Connected -> {
                    messagingManager = currentStatus.messagingManager
                    break
                }

                is Connecting -> {
                    logger.debug { "Suspending until WS connection is established..." }
                    suspendCancellableCoroutine {
                        currentStatus.coroutinesAwaitingConnection.add(it)
                    }
                }

                is NotConnected -> {
                    check(!reconnectAutomatically)
                    logger.debug { "Resuming messaging loop (automatic reconnect is disabled)" }
                    currentStatus.messagingLoopContinuation.resume(Unit)
                }

                NotInitialized -> throw IllegalStateException("${WebSocketRemotingClientImpl<*>::runMessagingLoop.name}() is not running.")
            }
        }

        return messagingManager?.block() ?: throw IllegalStateException()
    }

    override suspend fun runMessagingLoop(): Nothing {
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
            logger.debug { "Remote WS URL: " / wsUrl }

            while (true) {
                if (!reconnectAutomatically) {
                    logger.debug { "Suspending messaging loop (automatic reconnect is disabled)" }
                    suspendCancellableCoroutine {
                        status = NotConnected(it)
                    }
                }

                status = Connecting()

                var connectionIdForClosing: RemoteConnectionId? = null
                try {
                    httpSupportImplementor.runInSession(wsUrl, messageCodec) {
                        connectionIdForClosing = remoteConnectionId

                        val messagingManager = BidirectionalMessagingManagerImpl(
                            this,
                            messageCodec as MessageCodecWithMetadataPrefetchSupport<M>,
                            (incomingCallDelegators as Set<RemoteCallHandlerImplementor<*>>).associateBy { it.serviceId },
                            null
                        )

                        val previousStatus = status
                        check(previousStatus is Connecting)
                        status = Connected(messagingManager)

                        logger.debug { "WS connection is established, resuming coroutines awaiting connection." }
                        previousStatus.coroutinesAwaitingConnection.forEach {
                            it.resume(Unit)
                        }

                        peerRegistry.addConnection(
                            remoteConnectionId,
                            RemoteConnectionData(remoteConnectionId, DelegatingRemotingClient(messagingManager))
                        )
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
                        val previousStatus = status
                        if (previousStatus is Connected) {
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

    override suspend fun <T : Any, P : Any, R> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R =
        withMessagingManager {
            call(
                ServiceLocator(serviceId, methodId),
                parameter,
                parameterSerializer,
                resultDeserializer
            )
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
