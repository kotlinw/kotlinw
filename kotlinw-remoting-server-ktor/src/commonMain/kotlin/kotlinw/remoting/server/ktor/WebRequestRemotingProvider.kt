package kotlinw.remoting.server.ktor

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.contentType
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RawMessage.Binary
import kotlinw.remoting.core.RawMessage.Text
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.server.ktor.RemotingProvider.InstallationContext
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.uuid.Uuid
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.RemoteCallContextElement
import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor
import xyz.kotlinw.remoting.api.internal.RemotingMethodDescriptor.DownstreamColdFlow
import xyz.kotlinw.remoting.api.internal.RemotingMethodDescriptor.SynchronousCall

class WebRequestRemotingProvider(
    loggerFactory: LoggerFactory
) : RemotingProvider {

    private val logger = loggerFactory.getLogger()

    override fun InstallationContext.install() {
        val messageCodec = requireNotNull(messageCodec) { "Message codec is undefined." }

        val delegators =
            (remotingConfiguration.remoteCallHandlers as Iterable<RemoteCallHandlerImplementor<*>>).associateBy { it.serviceId }
        if (
            delegators.values.flatMap { it.methodDescriptors.values }.filterIsInstance<DownstreamColdFlow<*, *>>().any()
        ) {
            throw IllegalStateException("Downstream communication is not supported by ${WebRequestRemotingProvider::class}.")
        }

        if (remotingConfiguration.authenticationProviderName != null && ktorApplication.pluginOrNull(Authentication) == null) {
            // TODO install automatically instead of the error message
            throw IllegalStateException("Required Ktor plugin is not installed: ${Authentication.key.name}. It should be installed to support authentication provider '${remotingConfiguration.authenticationProviderName}' required by remoting provider '${remotingConfiguration.id}'.")
        }

        ktorApplication.routing {
            route("/remoting/${remotingConfiguration.id}") {// TODO lehessen testre szabni

                fun Route.configureRouting() {
                    setupRouting(messageCodec, delegators, remotingConfiguration as WebRequestRemotingConfiguration)
                }

                if (remotingConfiguration.authenticationProviderName != null) {
                    authenticate(remotingConfiguration.authenticationProviderName) {
                        logger.info { "Remote call handlers (authorization by '" / remotingConfiguration.authenticationProviderName / "'): " / delegators.mapValues { it.value.serviceId } }
                        configureRouting()
                    }
                } else {
                    logger.info { "Remote call handlers (no authorization): " / delegators.mapValues { it.value.serviceId } }
                    configureRouting()
                }
            }
        }
    }

    private fun Route.setupRouting(
        messageCodec: MessageCodec<out RawMessage>,
        remoteCallHandlers: Map<String, RemoteCallHandlerImplementor<*>>,
        remotingConfiguration: WebRequestRemotingConfiguration
    ) {
        val contentType = ContentType.parse(messageCodec.contentType)

        route("/call") { // TODO configurable path
            contentType(contentType) {
                remoteCallHandlers.forEach { (serviceId, handler) ->
                    handler.methodDescriptors.forEach { (methodId, methodDescriptor) ->
                        logger.trace { "Binding RPC call: " / serviceId / methodId }
                        post("/$serviceId/${methodDescriptor.memberId}") {
                            logger.trace { "Processing RPC call: " / serviceId / methodId }

                            when (methodDescriptor) {
                                is SynchronousCall<*, *> ->
                                    handleSynchronousCall(
                                        call,
                                        messageCodec,
                                        methodDescriptor,
                                        handler,
                                        remotingConfiguration
                                    )

                                is DownstreamColdFlow<*, *> ->
                                    throw AssertionError() // Validated in WebRequestRemotingProvider.install()
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun <M : RawMessage> handleSynchronousCall(
    call: ApplicationCall,
    messageCodec: MessageCodec<M>,
    callDescriptor: SynchronousCall<*, *>,
    delegator: RemoteCallHandlerImplementor<*>,
    remotingConfiguration: WebRequestRemotingConfiguration
) {
    val isBinaryCodec = messageCodec.isBinary

    val rawRequestMessage =
        if (isBinaryCodec) {
            RawMessage.Binary(call.receive<ByteArray>().view())
        } else {
            RawMessage.Text(call.receiveText())
        }

    val parameter = messageCodec.decodeMessage(
        rawRequestMessage as M,
        callDescriptor.parameterSerializer
    ).payload

    val principal = remotingConfiguration.extractPrincipal(call)
    val messagingPeerId = remotingConfiguration.identifyClient(call, principal) // TODO hibaell.
    val messagingConnectionId: MessagingConnectionId = Uuid.randomUuid().toString() // TODO customizable

    val result =
        withContext(
            RemoteCallContextElement(
                WebRequestRemoteCallContext(
                    RemoteConnectionId(
                        messagingPeerId,
                        messagingConnectionId
                    ),
                    call
                )
            )
        ) {
            delegator.processCall(callDescriptor.memberId, parameter)
        }

    val responseMessage = RemotingMessage(result, null) // TODO metadata
    val rawResponseMessage = messageCodec.encodeMessage(
        responseMessage,
        callDescriptor.resultSerializer as KSerializer<Any?>
    )

    call.response.status(HttpStatusCode.OK)
    call.response.header(HttpHeaders.ContentType, messageCodec.contentType)

    if (isBinaryCodec) {
        check(rawResponseMessage is Binary)
        call.respondBytes(rawResponseMessage.byteArrayView.toReadOnlyByteArray())
    } else {
        check(rawResponseMessage is Text)
        call.respondText(rawResponseMessage.text)
    }
}
