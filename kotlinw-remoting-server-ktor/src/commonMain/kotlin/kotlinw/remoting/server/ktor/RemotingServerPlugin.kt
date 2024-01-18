package kotlinw.remoting.server.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.codec.MessageCodec
import kotlinx.coroutines.CoroutineScope

class RemotingPluginConfiguration {

    var remotingConfigurations: Collection<RemotingConfiguration> = emptyList()

    var ktorServerCoroutineScope: CoroutineScope? = null

    var defaultMessageCodec: MessageCodec<out RawMessage>? = JsonMessageCodec.Default
}

private val logger by lazy { PlatformLogging.getLogger() }

private const val RemotingServerPluginName = "RemotingServer"

val RemotingServerPlugin =
    createApplicationPlugin(
        name = RemotingServerPluginName,
        createConfiguration = ::RemotingPluginConfiguration
    ) {
        if (pluginConfig.remotingConfigurations.isNotEmpty()) {
            pluginConfig.remotingConfigurations.forEach { remotingConfiguration ->
                remotingConfiguration.remotingProvider.installInternal(
                    object : RemotingProvider.InstallationContext {

                        override val remotingConfiguration get() = remotingConfiguration

                        override val ktorApplication get() = application

                        override val ktorServerCoroutineScope get() = pluginConfig.ktorServerCoroutineScope

                        override val messageCodec
                            get() = remotingConfiguration.messageCodec ?: pluginConfig.defaultMessageCodec
                    }
                )
            }
        } else {
            logger.warning { "Ktor RemotingServerPlugin is installed but no remoting configuration is defined." }
        }
    }

fun Application.installServerWebSockets(pingPeriod: Duration = 30.seconds) {
    install(WebSockets) {
        this.pingPeriod = pingPeriod.toJavaDuration()
    }
}
