package kotlinw.remoting.server.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinw.remoting.core.PayloadSerializer
import kotlinw.remoting.core.RemotingServerDelegateHelperImpl
import kotlinw.remoting.server.core.RemotingServerDelegate
import kotlinw.remoting.server.core.RemotingServerDelegate.Payload
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat

fun Routing.remotingServerRouting(
    payloadSerializer: PayloadSerializer<*, *>,
    remoteCallDelegators: Iterable<RemotingServerDelegate>
) {
    val contentTypeValue = payloadSerializer.contentType
    val contentType = ContentType.parse(contentTypeValue) // TODO check
    val isBinary = payloadSerializer is PayloadSerializer.BinaryPayloadSerializer

    val delegators = remoteCallDelegators.associateBy { it.servicePath }

    route("/remoting/call") {
        contentType(contentType) {
            post("/{serviceName}/{methodName}") {
                val serviceName = call.parameters["serviceName"]
                if (serviceName != null) {
                    val delegator = delegators[serviceName]
                    if (delegator != null) {
                        val methodName = call.parameters["methodName"]
                        if (methodName != null) {
                            val requestPayload =
                                if (isBinary) {
                                    Payload.Binary(call.receive<ByteArray>())
                                } else {
                                    Payload.Text(call.receiveText())
                                }

                            val responsePayload = delegator.processCall(methodName, requestPayload)

                            call.response.status(HttpStatusCode.OK)
                            call.response.header(HttpHeaders.ContentType, contentType.toString())

                            if (isBinary) {
                                call.respondBytes((responsePayload as Payload.Binary).byteArray)
                            } else {
                                call.respondText((responsePayload as Payload.Text).text)
                            }
                        }
                    }
                }
            }
        }
    }
}