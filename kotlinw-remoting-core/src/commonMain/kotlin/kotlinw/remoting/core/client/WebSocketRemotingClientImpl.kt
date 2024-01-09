package kotlinw.remoting.core.client

import arrow.atomic.AtomicBoolean
import arrow.core.nonFatalOrThrow
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.time.Duration.Companion.seconds
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.Connected
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.Connecting
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.Disconnected
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.InactiveMessagingStatus
import kotlinw.remoting.core.client.WebSocketRemotingClientImpl.BidirectionalMessagingStatus.MessageLoopSuspended
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
import kotlinw.util.stdlib.infiniteLoop
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    parentCoroutineContext: CoroutineContext,
    private val reconnectAutomatically: Boolean = true
) : RemotingClientCallSupport, RemotingClientFlowSupport, PersistentRemotingClient {

    private val logger = loggerFactory.getLogger()

    override val coroutineContext = parentCoroutineContext + SupervisorJob(parentCoroutineContext.job)

    private sealed interface BidirectionalMessagingStatus {

        sealed interface InactiveMessagingStatus : BidirectionalMessagingStatus {

            val coroutinesAwaitingConnection: PersistentList<Continuation<Unit>>
        }

        data class MessageLoopSuspended(
            val messagingLoopContinuation: Continuation<Unit>,
            override val coroutinesAwaitingConnection: PersistentList<Continuation<Unit>>
        ) :
            InactiveMessagingStatus

        data class Connecting(
            override val coroutinesAwaitingConnection: PersistentList<Continuation<Unit>>
        ) : InactiveMessagingStatus

        data class Disconnected(
            override val coroutinesAwaitingConnection: PersistentList<Continuation<Unit>>
        ) : InactiveMessagingStatus

        data class Connected(val messagingManager: BidirectionalMessagingManager) : BidirectionalMessagingStatus
    }

    private val status = atomic<BidirectionalMessagingStatus>(Disconnected(persistentListOf()))

    private val statusLock = Mutex()

    private val messagingLoopRunningFlag = AtomicBoolean(false)

    private suspend fun <T> withMessagingManager(block: suspend BidirectionalMessagingManager.() -> T): T {
        while (true) {
            statusLock.lock()
            val currentStatus = status.value

            if (currentStatus is Connected) {
                val messagingManager = currentStatus.messagingManager
                statusLock.unlock()
                return messagingManager.block()
            }

            try {
                check(currentStatus is InactiveMessagingStatus)

                logger.debug { "Suspending until WS connection is established..." }
                suspendCancellableCoroutine<Unit> { continuation ->
                    status.update {
                        check(it == currentStatus && it is InactiveMessagingStatus)
                        val newCoroutinesAwaitingConnection = it.coroutinesAwaitingConnection.add(continuation)
                        logger.debug { "Coroutines awaiting connection: " / newCoroutinesAwaitingConnection.size }
                        when (it) {
                            is Connecting -> Connecting(newCoroutinesAwaitingConnection)
                            is Disconnected -> Disconnected(newCoroutinesAwaitingConnection)
                            is MessageLoopSuspended -> Connecting(newCoroutinesAwaitingConnection)
                        }
                    }

                    if (currentStatus is MessageLoopSuspended) {
                        logger.debug { "Resuming messaging loop." }
                        currentStatus.messagingLoopContinuation.resume(Unit)
                    }

                    statusLock.unlock()
                }
            } catch (e: Throwable) {
                statusLock.unlock() // Should not reach this line
                throw e
            }
        }
    }

    override suspend fun runMessagingLoop(): Nothing {
        if (messagingLoopRunningFlag.compareAndSet(false, true)) {
            try {
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

                infiniteLoop {
                    if (reconnectAutomatically) {
                        statusLock.withLock {
                            status.update {
                                Connecting(
                                    if (it is InactiveMessagingStatus) it.coroutinesAwaitingConnection else persistentListOf()
                                )
                            }
                        }
                    } else {
                        try {
                            statusLock.lock()
                            logger.debug { "Suspending messaging loop (automatic reconnect is disabled)" }
                            suspendCancellableCoroutine { continuation ->
                                status.update {
                                    MessageLoopSuspended(
                                        continuation,
                                        if (it is InactiveMessagingStatus) it.coroutinesAwaitingConnection else persistentListOf()
                                    )
                                }
                                statusLock.unlock()
                            }
                        } catch (e: Throwable) {
                            statusLock.unlock() // Should not reach this line
                            throw e
                        }
                    }

                    var connectionIdForClosing: RemoteConnectionId? = null
                    try {
                        coroutineScope {
                            httpSupportImplementor.runInSession(wsUrl, messageCodec) {
                                connectionIdForClosing = remoteConnectionId

                                val messagingManager = BidirectionalMessagingManagerImpl(
                                    this,
                                    messageCodec as MessageCodecWithMetadataPrefetchSupport<M>,
                                    (incomingCallDelegators as Set<RemoteCallHandlerImplementor<*>>).associateBy { it.serviceId },
                                    null
                                )

                                val previousStatus =
                                    statusLock.withLock {
                                        status.getAndUpdate {
                                            check(it is Connecting)
                                            Connected(messagingManager)
                                        }
                                    }

                                val coroutinesAwaitingConnection =
                                    (previousStatus as Connecting).coroutinesAwaitingConnection
                                logger.debug { "WS connection is established, resuming " / coroutinesAwaitingConnection.size / " coroutines awaiting connection." }
                                coroutinesAwaitingConnection.forEach {
                                    it.resume(Unit)
                                }

                                peerRegistry.addConnection(
                                    remoteConnectionId,
                                    RemoteConnectionData(remoteConnectionId, DelegatingRemotingClient(messagingManager))
                                )

                                messagingManager.processIncomingMessages()
                            }
                        }
                    } catch (e: Throwable) {
                        if (e is CancellationException) {
                            logger.debug { "WebSocket connection terminated." } // TODO melyik fél által?

                            statusLock.withLock {
                                status.update {
                                    Disconnected(persistentListOf())
                                }
                            }

                            throw e
                        } else {
                            val cause = e.nonFatalOrThrow()
                            // TODO specifikus exception-ök külön elkapása runInSession()-ben

                            if (logger.isTraceEnabled) {
                                logger.trace(cause) { "Connection failed." }
                            } else {
                                logger.debug { "Connection failed: " / cause.message }
                            }
                        }
                    } finally {
                        if (connectionIdForClosing != null) {
                            peerRegistry.removeConnection(connectionIdForClosing!!)
                            connectionIdForClosing = null
                        }
                    }

                    statusLock.withLock {
                        status.update {
                            Disconnected(
                                if (it is InactiveMessagingStatus) it.coroutinesAwaitingConnection else persistentListOf()
                            )
                        }
                    }

                    if (reconnectAutomatically) {
                        val waitDuration = 1.seconds // TODO config
                        logger.debug { "Waiting " / waitDuration / " before reconnecting automatically..." }
                        delay(waitDuration)
                    }
                }
            } finally {
                messagingLoopRunningFlag.value = false
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

    override suspend fun close() {
        cancel()
    }
}
