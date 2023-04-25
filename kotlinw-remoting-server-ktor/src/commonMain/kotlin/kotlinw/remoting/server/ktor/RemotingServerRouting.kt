package kotlinw.remoting.server.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinw.remoting.core.PayloadSerializer
import kotlinw.remoting.server.core.RemoteCallDelegator
import kotlinw.remoting.server.core.RawMessage

fun Routing.remotingServerRouting(
    payloadSerializer: PayloadSerializer<*, *>,
    remoteCallDelegators: Iterable<RemoteCallDelegator>
) {
    val contentTypeValue = payloadSerializer.contentType
    val contentType = ContentType.parse(contentTypeValue) // TODO check
    val isBinary = payloadSerializer is PayloadSerializer.BinaryPayloadSerializer

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

                            val responsePayload = delegator.processCall(methodPath, requestPayload) // TODO handle errors

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