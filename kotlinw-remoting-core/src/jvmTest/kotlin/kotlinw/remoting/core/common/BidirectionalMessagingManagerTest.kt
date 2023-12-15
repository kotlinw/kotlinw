package kotlinw.remoting.core.common

import app.cash.turbine.turbineScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import xyz.kotlinw.remoting.api.internal.RemotingMethodDescriptor
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.serializer
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingSessionId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor

class BidirectionalMessagingManagerTest {

    private class BidirectionalMessagingPipe(
        private val coroutineScope: CoroutineScope
    ) : CoroutineScope by coroutineScope {

        private val messagingSessionId = Uuid.randomUuid().toString()

        private val peer1IncomingMessagesFlow = MutableSharedFlow<RawMessage>()

        private val peer2IncomingMessagesFlow = MutableSharedFlow<RawMessage>()

        val peer1 = object : BidirectionalMessagingConnection {
            override val peerId: MessagingPeerId get() = "peer1"

            override val sessionId: MessagingSessionId = "$peerId-$messagingSessionId"

            override suspend fun incomingRawMessages(): Flow<RawMessage> = peer1IncomingMessagesFlow

            override suspend fun sendRawMessage(rawMessage: RawMessage) {
                peer2IncomingMessagesFlow.emit(rawMessage)
            }

            override suspend fun close() {}

            override val coroutineContext: CoroutineContext get() = coroutineScope.coroutineContext
        }

        val peer2 = object : BidirectionalMessagingConnection {
            override val peerId: MessagingPeerId get() = "peer2"

            override val sessionId: MessagingSessionId = "$peerId-$messagingSessionId"

            override suspend fun incomingRawMessages(): Flow<RawMessage> = peer2IncomingMessagesFlow

            override suspend fun sendRawMessage(rawMessage: RawMessage) {
                peer1IncomingMessagesFlow.emit(rawMessage)
            }

            override suspend fun close() {}

            override val coroutineContext: CoroutineContext get() = coroutineScope.coroutineContext
        }

        suspend fun sendMessageFromPeer1ToPeer2(rawMessage: RawMessage) {
            peer1.sendRawMessage(rawMessage)
        }

        suspend fun sendMessageFromPeer2ToPeer1(rawMessage: RawMessage) {
            peer2.sendRawMessage(rawMessage)
        }
    }

    @Test
    fun testBidirectionalMessagingPipe() = runTest {
        turbineScope {
            val pipe = BidirectionalMessagingPipe(backgroundScope)
            val peer1Turbine = pipe.peer1.incomingRawMessages().testIn(backgroundScope)
            val peer2Turbine = pipe.peer2.incomingRawMessages().testIn(backgroundScope)

            pipe.sendMessageFromPeer1ToPeer2(RawMessage.Text("a"))
            assertEquals("a", (peer2Turbine.awaitItem() as RawMessage.Text).text)

            pipe.sendMessageFromPeer2ToPeer1(RawMessage.Text("b"))
            assertEquals("b", (peer1Turbine.awaitItem() as RawMessage.Text).text)
        }
    }

    @Test
    fun testPeer1ToPeer2Call() = runTest {
        val peer2RemoteCallHandler = mockk<RemoteCallHandlerImplementor>()
        every { peer2RemoteCallHandler.servicePath } answers { "peer2Service" }
        every { peer2RemoteCallHandler.methodDescriptors } answers {
            setOf(
                RemotingMethodDescriptor.SynchronousCall(
                    "peer2Method",
                    serializer<Unit>(),
                    serializer<Unit>()
                )
            ).associateBy { it.memberId }
        }
        coEvery { peer2RemoteCallHandler.processCall("peer2Method", Unit) } coAnswers { Unit }

        val pipe = BidirectionalMessagingPipe(backgroundScope)
        val messageCodec = JsonMessageCodec.Default
        val peer1Manager = BidirectionalMessagingManagerImpl(pipe.peer1, messageCodec, emptyMap())
        val peer2Manager = BidirectionalMessagingManagerImpl(
            pipe.peer2,
            messageCodec,
            setOf(peer2RemoteCallHandler).associateBy { it.servicePath })

        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            peer1Manager.processIncomingMessages()
        }
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            peer2Manager.processIncomingMessages()
        }

        peer1Manager.call(
            ServiceLocator("peer2Service", "peer2Method"),
            Unit,
            serializer<Unit>(),
            serializer<Unit>()
        )
    }

    @Test
    fun testPeer2ToPeer1Call() = runTest {
        val peer1RemoteCallHandler = mockk<RemoteCallHandlerImplementor>()
        every { peer1RemoteCallHandler.servicePath } answers { "peer1Service" }
        every { peer1RemoteCallHandler.methodDescriptors } answers {
            setOf(
                RemotingMethodDescriptor.SynchronousCall(
                    "peer1Method",
                    serializer<Unit>(),
                    serializer<Unit>()
                )
            ).associateBy { it.memberId }
        }
        coEvery { peer1RemoteCallHandler.processCall("peer1Method", Unit) } coAnswers { Unit }

        val pipe = BidirectionalMessagingPipe(backgroundScope)
        val messageCodec = JsonMessageCodec.Default
        val peer1Manager = BidirectionalMessagingManagerImpl(
            pipe.peer1,
            messageCodec,
            setOf(peer1RemoteCallHandler).associateBy { it.servicePath })
        val peer2Manager = BidirectionalMessagingManagerImpl(pipe.peer2, messageCodec, emptyMap())

        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            peer1Manager.processIncomingMessages()
        }
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            peer2Manager.processIncomingMessages()
        }

        peer2Manager.call(
            ServiceLocator("peer1Service", "peer1Method"),
            Unit,
            serializer<Unit>(),
            serializer<Unit>()
        )
    }

    @Test
    fun testPeer1CollectingFlowProducedByPeer2() = runTest {
        val peer2RemoteCallHandler = mockk<RemoteCallHandlerImplementor>()
        every { peer2RemoteCallHandler.servicePath } answers { "peer2Service" }
        every { peer2RemoteCallHandler.methodDescriptors } answers {
            setOf(
                RemotingMethodDescriptor.DownstreamColdFlow(
                    "peer2Method",
                    serializer<Unit>(),
                    serializer<Int>()
                )
            ).associateBy { it.memberId }
        }
        coEvery {
            peer2RemoteCallHandler.processCall("peer2Method", Unit)
        } coAnswers {
            flow {
                (1..3).forEach {
                    println("preparing: $it")
                    emit(it)
                    println("emitted: $it")
                }
            }
        }

        val pipe = BidirectionalMessagingPipe(backgroundScope)
        val messageCodec = JsonMessageCodec.Default
        val peer1Manager = BidirectionalMessagingManagerImpl(pipe.peer1, messageCodec, emptyMap())
        val peer2Manager = BidirectionalMessagingManagerImpl(
            pipe.peer2,
            messageCodec,
            setOf(peer2RemoteCallHandler).associateBy { it.servicePath })

        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            peer1Manager.processIncomingMessages()
        }
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            peer2Manager.processIncomingMessages()
        }

        assertEquals(
            listOf(1, 2, 3),
            peer1Manager.requestColdFlowResult(
                ServiceLocator("peer2Service", "peer2Method"),
                Unit,
                serializer<Unit>(),
                serializer<Int>(),
                Uuid.randomUuid().toString()
            ).toList()
        )
    }
}
