package kotlinw.remoting.server.ktor

import io.ktor.server.application.Application
import kotlinw.remoting.core.codec.MessageCodec
import kotlinx.coroutines.CoroutineScope

interface RemotingProvider {

    interface InstallationContext {

        val ktorApplication: Application

        val ktorServerCoroutineScope: CoroutineScope?

        val messageCodec: MessageCodec<*>?

        val remotingConfiguration: RemotingConfiguration
    }

    // TODO context receivers
    fun InstallationContext.install()

    // TODO context receivers: remove
    fun installInternal(context: InstallationContext) = context.install()
}
