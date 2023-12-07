package kotlinw.remoting.server.ktor

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.contentType
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.api.internal.server.RemoteCallHandler
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor.DownstreamColdFlow
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.server.ktor.RemotingProvider.InstallationContext
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

class WebRequestRemotingProvider : RemotingProvider {

    private val logger by lazy { PlatformLogging.getLogger() }

    override fun InstallationContext.install() {
        val messageCodec = requireNotNull(messageCodec) { "Message codec is undefined." }

        val delegators = remoteCallHandlers.associateBy { it.servicePath }
        if (
            delegators.values.flatMap { it.methodDescriptors.values }.filterIsInstance<DownstreamColdFlow<*, *>>().any()
        ) {
            throw IllegalStateException("Downstream communication is not supported by ${WebRequestRemotingProvider::class}.")
        }

        ktorApplication.routing {
            route("/remoting") {

                fun Route.configureRouting() {
                    setupRouting(messageCodec, delegators)
                }

                if (authenticationProviderName != null) {
                    authenticate(authenticationProviderName) {
                        configureRouting()
                    }
                } else {
                    configureRouting()
                }
            }
        }
    }

    private fun Route.setupRouting(
        messageCodec: MessageCodec<out RawMessage>,
        remoteCallHandlers: Map<String, RemoteCallHandler>
    ) {
        val contentType = ContentType.parse(messageCodec.contentType)

        route("/call") { // TODO configurable path
            contentType(contentType) {
                logger.info { "Remote call handlers: " / remoteCallHandlers }
                post("/{serviceId}/{methodId}") {
                    // TODO handle errors

                    val serviceId = call.parameters["serviceId"]
                    if (serviceId != null) {
                        val delegator = remoteCallHandlers[serviceId]
                        if (delegator != null) {
                            val methodId = call.parameters["methodId"]
                            if (methodId != null) {
                                val methodDescriptor = delegator.methodDescriptors[methodId]
                                if (methodDescriptor != null) {
                                    logger.trace { "Processing RPC call: " / serviceId / methodId }

                                    when (methodDescriptor) {
                                        is RemotingMethodDescriptor.DownstreamColdFlow<*, *> -> {
                                            logger.warning { "Remoting methods with ${Flow::class.simpleName} return types are not supported by this endpoint." }
                                            call.response.status(HttpStatusCode.BadRequest)
                                        }

                                        is RemotingMethodDescriptor.SynchronousCall<*, *> ->
                                            handleSynchronousCall(
                                                call,
                                                messageCodec,
                                                methodDescriptor,
                                                delegator
                                            )
                                    }
                                } else {
                                    logger.warning {
                                        "Invalid incoming RPC call, handler does not support the requested method: " /
                                                listOf(serviceId, methodId)
                                    }
                                }
                            } else {
                                logger.warning {
                                    "Invalid incoming RPC call, no `methodId` present: " /
                                            named("serviceId", serviceId) / call.request.uri
                                }
                            }
                        } else {
                            logger.warning {
                                "Invalid incoming RPC call, no handler found for " /
                                        named("serviceId", serviceId)
                            }
                        }
                    } else {
                        logger.warning { "Invalid incoming RPC call, no `serviceId` present: " / call.request.uri }
                    }
                }
            }
        }
    }

    private suspend fun <M : RawMessage> handleSynchronousCall(
        call: ApplicationCall,
        messageCodec: MessageCodec<M>,
        callDescriptor: RemotingMethodDescriptor.SynchronousCall<*, *>,
        delegator: RemoteCallHandler
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

        val result = delegator.processCall(callDescriptor.memberId, parameter)

        val responseMessage = RemotingMessage(result, null) // TODO metadata
        val rawResponseMessage = messageCodec.encodeMessage(
            responseMessage,
            callDescriptor.resultSerializer as KSerializer<Any?>
        )

        call.response.status(HttpStatusCode.OK)
        call.response.header(HttpHeaders.ContentType, messageCodec.contentType)

        if (isBinaryCodec) {
            check(rawResponseMessage is RawMessage.Binary)
            call.respondBytes(rawResponseMessage.byteArrayView.toReadOnlyByteArray())
        } else {
            check(rawResponseMessage is RawMessage.Text)
            call.respondText(rawResponseMessage.text)
        }
    }
}
