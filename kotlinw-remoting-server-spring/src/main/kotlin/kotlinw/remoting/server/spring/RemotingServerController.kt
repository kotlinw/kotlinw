package kotlinw.remoting.server.spring

import jakarta.annotation.PostConstruct
import kotlinw.remoting.server.core.RemoteCallDelegator
import kotlinw.remoting.server.core.RawMessage
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
    suspend fun call(
        @PathVariable serviceName: String,
        @PathVariable methodName: String,
        @RequestBody requestBody: String
    ): String {
        val service = handlers[serviceName]
        if (service != null) {
            val responsePayload =
                service.processCall(methodName, RawMessage.Text(requestBody)) // TODO handle errors, eg. method not found

            if (responsePayload is RawMessage.Text) {
                return responsePayload.text
            } else {
                throw IllegalStateException("Expected response payload of type Payload.Text but got: $responsePayload")
            }
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
