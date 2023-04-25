package kotlinw.remoting.server.ktor

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.contentType
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinw.remoting.core.MessageCodecDescriptor
import kotlinw.remoting.core.MessageCodecImpl
import kotlinw.remoting.server.core.RawMessage
import kotlinw.remoting.server.core.RemoteCallDelegator

fun Routing.remotingServerRouting(
    messageCodecDescriptor: MessageCodecDescriptor,
    remoteCallDelegators: Iterable<RemoteCallDelegator>
) {
    val contentTypeValue = messageCodecDescriptor.contentType
    val contentType = ContentType.parse(contentTypeValue) // TODO check
    val isBinary = messageCodecDescriptor is MessageCodecDescriptor.Binary
    val messageSerializer = MessageCodecImpl(messageCodecDescriptor)

    val delegators = remoteCallDelegators.associateBy { it.servicePath }

    route("/remoting/call") { // TODO configurable
        contentType(contentType) {
            post("/{servicePath}/{methodPath}") {
                val servicePath = call.parameters["servicePath"]
                if (servicePath != null) {
                    val delegator = delegators[servicePath]
                    if (delegator != null) {
                        val methodPath = call.parameters["methodPath"]
                        if (methodPath != null) {
                            val requestPayload =
                                if (isBinary) {
                                    RawMessage.Binary(call.receive<ByteArray>())
                                } else {
                                    RawMessage.Text(call.receiveText())
                                }

                            // TODO handle errors
                            val responsePayload = delegator.processCall(methodPath, requestPayload, messageSerializer)

                            call.response.status(HttpStatusCode.OK)
                            call.response.header(HttpHeaders.ContentType, contentType.toString())

                            if (isBinary) {
                                call.respondBytes((responsePayload as RawMessage.Binary).byteArray)
                            } else {
                                call.respondText((responsePayload as RawMessage.Text).text)
                            }
                        }
                    }
                }
            }
        }
    }
}