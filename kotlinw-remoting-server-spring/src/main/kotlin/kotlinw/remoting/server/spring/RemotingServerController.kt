package kotlinw.remoting.server.spring

import jakarta.annotation.PostConstruct
import kotlinw.remoting.core.MessageCodec
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.server.core.RemoteCallDelegator
import kotlinx.serialization.KSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException

@Controller
@RequestMapping("\${kotlinw.remoting.server.spring.controller.path:/remoting}")
class RemotingServerController {

    @Autowired
    private lateinit var messageCodec: MessageCodec

    @Autowired
    private lateinit var handlerList: List<RemoteCallDelegator>

    private lateinit var handlers: Map<String, RemoteCallDelegator>

    @PostConstruct
    fun initialize() {
        handlers = handlerList.associateBy { it.servicePath }
    }

    @PostMapping(
        path = ["/call/{serviceName}/{methodName}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    suspend fun callText(
        @PathVariable serviceName: String,
        @PathVariable methodName: String,
        @RequestBody requestBody: String
    ): String {
        // TODO handle errors

        val service = handlers[serviceName]
        if (service != null) {
            val methodDescriptor = service.methodDescriptors[methodName]
            if (methodDescriptor != null) {
                val parameter =
                    messageCodec.decodeMessage(RawMessage.Text(requestBody), methodDescriptor.parameterSerializer)
                val result = service.processCall(methodName, parameter)
                return (messageCodec.encodeMessage(
                    result,
                    methodDescriptor.resultSerializer as KSerializer<Any>
                ) as RawMessage.Text).text
            } else {
                throw ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(
        path = ["/call/{serviceName}/{methodName}"],
        consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    @ResponseBody
    suspend fun callBinary(
        @PathVariable serviceName: String,
        @PathVariable methodName: String,
        @RequestBody requestBody: ByteArray
    ): ByteArray {
        // TODO handle errors

        val service = handlers[serviceName]
        if (service != null) {
            val methodDescriptor = service.methodDescriptors[methodName]
            if (methodDescriptor != null) {
                val parameter =
                    messageCodec.decodeMessage(RawMessage.Binary(requestBody), methodDescriptor.parameterSerializer)
                val result = service.processCall(methodName, parameter)
                return (messageCodec.encodeMessage(
                    result,
                    methodDescriptor.resultSerializer as KSerializer<Any>
                ) as RawMessage.Binary).byteArray
            } else {
                throw ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
