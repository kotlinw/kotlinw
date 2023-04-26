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
import kotlinw.remoting.core.MessageCodec
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.server.core.RemoteCallDelegator
import kotlinx.serialization.KSerializer

fun <M: RawMessage> Routing.remotingServerRouting(
    messageCodec: MessageCodec<M>,
    remoteCallDelegators: Iterable<RemoteCallDelegator>
) {
    val contentTypeValue = messageCodec.contentType
    val contentType = ContentType.parse(contentTypeValue)
    val isBinaryCodec = messageCodec.isBinary

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
                            val methodDescriptor = delegator.methodDescriptors[methodPath]
                            if (methodDescriptor != null) {
                                // TODO handle errors

                                val rawRequestMessage =
                                    if (isBinaryCodec) {
                                        RawMessage.Binary(call.receive<ByteArray>()) as M
                                    } else {
                                        RawMessage.Text(call.receiveText()) as M
                                    }

                                val requestMessage =
                                    messageCodec.decodeMessage(rawRequestMessage, methodDescriptor.parameterSerializer)

                                val result = delegator.processCall(methodPath, requestMessage.payload)

                                val responseMessage = RemotingMessage(result, null)
                                val rawResponseMessage = messageCodec.encodeMessage(
                                    responseMessage,
                                    methodDescriptor.resultSerializer as KSerializer<Any>
                                )

                                call.response.status(HttpStatusCode.OK)
                                call.response.header(HttpHeaders.ContentType, contentType.toString())

                                if (isBinaryCodec) {
                                    call.respondBytes((rawResponseMessage as RawMessage.Binary).byteArray)
                                } else {
                                    call.respondText((rawResponseMessage as RawMessage.Text).text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}