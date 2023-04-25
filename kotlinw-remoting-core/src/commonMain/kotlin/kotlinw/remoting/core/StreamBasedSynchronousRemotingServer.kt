package kotlinw.remoting.core

import kotlinw.remoting.server.core.RemoteCallDelegator
import okio.Sink
import okio.Source
import okio.buffer

class StreamBasedSynchronousRemotingServer(
    private val messageCodec: MessageCodec,
    private val source: Source,
    private val sink: Sink,
    private val handlers: List<RemoteCallDelegator>
) {

    private val handlerMap = handlers.associateBy { it.servicePath }

    private val bufferedSource = source.buffer()

    private val bufferedSink = sink.buffer()

    suspend fun listen(): Nothing {
        TODO()
//        while (true) {
//            val rawMessageSize = bufferedSource.readInt()
//            val rawMessageBytes = bufferedSource.readByteArray(rawMessageSize.toLong())
//
//
//
//            val service = handlers[serviceName]
//            if (service != null) {
//                val responsePayload =
//                    service.processCall(methodName, RawMessage.Text(requestBody), messageCodec) // TODO handle errors, eg. method not found
//
//                if (responsePayload is RawMessage.Text) {
//                    return responsePayload.text
//                } else {
//                    throw IllegalStateException("Expected response payload of type Payload.Text but got: $responsePayload")
//                }
//            } else {
//                throw ResponseStatusException(HttpStatus.NOT_FOUND);
//            }
//
//        }
    }
}
