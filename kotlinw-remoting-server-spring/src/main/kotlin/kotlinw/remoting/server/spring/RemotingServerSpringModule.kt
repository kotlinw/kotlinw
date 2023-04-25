package kotlinw.remoting.server.spring

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan
class RemotingServerSpringModule
