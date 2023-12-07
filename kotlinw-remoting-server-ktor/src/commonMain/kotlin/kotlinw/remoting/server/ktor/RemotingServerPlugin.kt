package kotlinw.remoting.server.ktor

import io.ktor.server.application.*
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.api.internal.server.RemoteCallHandler
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RawMessage.Text
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageDecoder
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
            pluginConfig.remotingConfigurations.forEach {
                it.remotingProvider.installInternal(
                    object : RemotingProvider.InstallationContext {

                        override val ktorApplication get() = application

                        override val ktorServerCoroutineScope get() = pluginConfig.ktorServerCoroutineScope

                        override val messageCodec get() = it.messageCodec ?: pluginConfig.defaultMessageCodec

                        override val remoteCallHandlers get() = it.remoteCallHandlers

                        override val authenticationProviderName get() = it.authenticationProviderName
                    }
                )
            }
        } else {
            logger.warning { "Ktor RemotingServerPlugin is installed but no remoting configuration is defined." }
        }
    }
