package kotlinw.remoting.core.client

import arrow.core.nonFatalOrThrow
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.core.RawMessage
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
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.infiniteLoop
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.kotlinw.remoting.api.PersistentRemotingClient
import xyz.kotlinw.remoting.api.PersistentRemotingConnection
import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor

class WebSocketRemotingClientImpl<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpSupportImplementor: SynchronousCallSupport,
    private val peerRegistry: MutableRemotePeerRegistry,
    private val remoteServerBaseUrl: Url,
    private val endpointId: String,
    private val incomingCallDelegators: Set<RemoteCallHandler<*>>,
    loggerFactory: LoggerFactory,
    private val reconnectAutomatically: Boolean = true
) : PersistentRemotingClient {

    private val logger = loggerFactory.getLogger()

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

    private val _status = MutableStateFlow<BidirectionalMessagingStatus>(Disconnected(persistentListOf()))

    override val isConnected: Boolean get() = _status.value is Connected

    private val statusLock = Mutex()

    private val messagingLoopRunningFlag = atomic(false)

    override val isConnectedStateFlow = _status.map { it is Connected }

    private suspend fun <T> withMessagingManager(block: suspend BidirectionalMessagingManager.() -> T): T {
        while (true) {
            statusLock.lock()
            val currentStatus = _status.value

            if (currentStatus is Connected) {
                val messagingManager = currentStatus.messagingManager
                statusLock.unlock()
                return messagingManager.block()
            }

            check(currentStatus is InactiveMessagingStatus)
            withContext(NonCancellable) {
                logger.debug { "Suspending until WS connection is established..." }
                suspendCancellableCoroutine<Unit> { continuation ->
                    _status.update {
                        check(it == currentStatus && it is InactiveMessagingStatus)
                        val newCoroutinesAwaitingConnection = it.coroutinesAwaitingConnection.add(continuation)
                        logger.debug { "Coroutines awaiting connection: " / newCoroutinesAwaitingConnection.size }
                        when (it) {
                            is Connecting -> Connecting(newCoroutinesAwaitingConnection)
                            is Disconnected -> Disconnected(newCoroutinesAwaitingConnection)
                            is MessageLoopSuspended -> Connecting(newCoroutinesAwaitingConnection)
                        }
                    }

                    continuation.invokeOnCancellation {
                        _status.update {
                            if (it is InactiveMessagingStatus) {
                                val newCoroutinesAwaitingConnection =
                                    it.coroutinesAwaitingConnection.remove(continuation)
                                when (it) {
                                    is Connecting -> it.copy(coroutinesAwaitingConnection = newCoroutinesAwaitingConnection)
                                    is Disconnected -> it.copy(coroutinesAwaitingConnection = newCoroutinesAwaitingConnection)
                                    is MessageLoopSuspended -> it.copy(coroutinesAwaitingConnection = newCoroutinesAwaitingConnection)
                                }
                            } else {
                                it
                            }
                        }
                    }

                    if (currentStatus is MessageLoopSuspended) {
                        logger.debug { "Resuming messaging loop." }
                        currentStatus.messagingLoopContinuation.resume(Unit)
                    }

                    statusLock.unlock()
                }
            }
        }
    }

    override suspend fun connectAndRunMessageLoop(
        handleException: suspend (Throwable) -> Unit,
        beforeAutomaticReconnect: suspend () -> Unit,
        globalNetworkStatusFlow: Flow<Boolean>?
    ): Nothing {
        if (messagingLoopRunningFlag.compareAndSet(false, true)) {
            try {
                check(httpSupportImplementor is BidirectionalCommunicationImplementor)

                val wsUrl = Url(
                    "${
                        remoteServerBaseUrl.value.replace( // TODO
                            "http",
                            "ws"
                        )
                    }/remoting/websocket/$endpointId" // TODO fix string
                )
                logger.debug { "Remote WS URL: " / wsUrl }

                infiniteLoop {
                    globalNetworkStatusFlow?.first { it }

                    if (reconnectAutomatically) {
                        statusLock.withLock {
                            _status.update {
                                Connecting(
                                    if (it is InactiveMessagingStatus) it.coroutinesAwaitingConnection else persistentListOf()
                                )
                            }
                        }
                    } else {
                        withContext(NonCancellable) {
                            statusLock.lock()
                            logger.debug { "Suspending messaging loop (automatic reconnect is disabled)" }
                            suspendCancellableCoroutine { continuation ->
                                _status.update {
                                    MessageLoopSuspended(
                                        continuation,
                                        if (it is InactiveMessagingStatus) it.coroutinesAwaitingConnection else persistentListOf()
                                    )
                                }
                                statusLock.unlock()
                            }
                        }
                    }

                    var connectionIdForClosing: RemoteConnectionId? = null
                    try {
                        coroutineScope { // TODO ez a coroutineScope() hívás miért is kell ide? merthogy enélkül nem működik :\
                            httpSupportImplementor.runInSession(wsUrl, messageCodec) {
                                connectionIdForClosing = remoteConnectionId

                                val messagingManager = BidirectionalMessagingManagerImpl(
                                    this,
                                    messageCodec as MessageCodecWithMetadataPrefetchSupport<M>,
                                    (incomingCallDelegators as Set<RemoteCallHandlerImplementor<*>>).associateBy { it.serviceId }
                                )

                                val previousStatus =
                                    statusLock.withLock {
                                        _status.getAndUpdate {
                                            check(it is Connecting)
                                            Connected(messagingManager)
                                        }
                                    }

                                val coroutinesAwaitingConnection =
                                    (previousStatus as Connecting).coroutinesAwaitingConnection
                                logger.debug { "Remoting WS connection is established, resuming " / coroutinesAwaitingConnection.size / " coroutines awaiting connection." }
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
                        // TODO itt a HTTP kliens konfigurációs hibákat ki kell szűrni, pl. ha nincs telepítve a kliens WebSockets plugin

                        if (e is CancellationException) {
                            logger.debug { "Remoting WS connection scope cancelled." } // TODO melyik fél általi megszakításkor jutunk ide?

                            statusLock.withLock {
                                _status.update {
                                    Disconnected(persistentListOf())
                                }
                            }

                            throw e
                        } else {
                            val cause = e.nonFatalOrThrow()
                            // TODO specifikus exception-ök külön elkapása runInSession()-ben

                            if (logger.isTraceEnabled) {
                                logger.trace(cause) { "Remoting WS connection failed." }
                            } else {
                                logger.debug {
                                    "Remoting WS connection failed: " / named("cause", cause::class.simpleName) /
                                            (if (cause.message != null) ": " / named("message", cause.message) else "")
                                }
                            }

                            handleException(e)
                        }
                    } finally {
                        if (connectionIdForClosing != null) {
                            peerRegistry.removeConnection(connectionIdForClosing!!)
                            connectionIdForClosing = null
                        }
                    }

                    statusLock.withLock {
                        _status.update {
                            Disconnected(
                                if (it is InactiveMessagingStatus) it.coroutinesAwaitingConnection else persistentListOf()
                            )
                        }
                    }

                    if (reconnectAutomatically) {
                        beforeAutomaticReconnect()
                    }
                }
            } finally {
                messagingLoopRunningFlag.value = false
            }
        } else {
            throw IllegalStateException("Messaging loop is already running.")
        }
    }

    // FIXME ezt átnézni, hogy tényleg jól működik-e, és util-ba kitenni
    @OptIn(InternalCoroutinesApi::class)
    override suspend fun <T> withConnection(block: suspend (PersistentRemotingConnection) -> T): Result<T> {
        val deferred = withMessagingManager {
            async(start = UNDISPATCHED) {
                block(PersistentRemotingConnectionImpl(this@withMessagingManager))
            }
        }

        return try {
            deferred.join()
            if (deferred.isCancelled) {
                Result.failure(deferred.getCancellationException())
            } else {
                Result.success(deferred.getCompleted())
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                deferred.cancelAndJoin()
            }
            throw e
        } catch (e: Throwable) {
            Result.failure(e.nonFatalOrThrow())
        }
    }
}
