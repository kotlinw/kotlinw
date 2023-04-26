package kotlinw.remoting.server.spring

import jakarta.annotation.PostConstruct
import kotlinw.remoting.core.MessageCodec
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
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
    private lateinit var messageCodec: MessageCodec<RawMessage.Text>

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
                val requestMessage =
                    messageCodec.decodeMessage(RawMessage.Text(requestBody), methodDescriptor.parameterSerializer)
                // TODO metadata
                val result = service.processCall(methodName, requestMessage.payload)
                val responseMessage = RemotingMessage(result, null) // TODO metadata
                return messageCodec.encodeMessage(
                    responseMessage,
                    methodDescriptor.resultSerializer as KSerializer<Any>
                ).text
            } else {
                throw ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    // TODO binary support
}
