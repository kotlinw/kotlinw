package kotlinw.module.serverbase

import io.ktor.server.application.*
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.websocket.*
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.getConfigurationPropertyTypedValue
import kotlinw.configuration.core.getConfigurationPropertyValue
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.dispatch
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.remoting.core.common.RemoteConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.koin.core.api.getAllSortedByPriority
import kotlinw.koin.core.api.registerShutdownTask
import kotlinw.koin.core.api.registerStartupTask
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.server.ktor.RemotingServerPlugin
import kotlinw.remoting.server.ktor.ServerToClientCommunicationType
import kotlinw.util.coroutine.createNestedSupervisorScope
import kotlinx.coroutines.launch
import org.koin.dsl.module

val serverBaseModule by lazy {
    module {
        single<ApplicationEngine>(createdAtStart = true) {
            val logger = get<LoggerFactory>().getLogger()
            val eventBus = get<LocalEventBus>()
            val remotePeerRegistry = get<MutableRemotePeerRegistry>()

            val ktorServerCoroutineScope =
                get<ApplicationCoroutineService>().coroutineScope.createNestedSupervisorScope()

            val environment = applicationEngineEnvironment {
                this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext

                this.log = KtorSimpleLogger(logger.name)

                this.module {
                    install(WebSockets)

                    install(RemotingServerPlugin) {
                        this.messageCodec = get<MessageCodec<*>>()
                        this.remoteCallDelegators = getAll<RemoteCallDelegator>()
                        this.identifyClient = { 1 } // FIXME
                        this.supportedServerToClientCommunicationTypes =
                            setOf(ServerToClientCommunicationType.WebSockets) // TODO configurable
                        this.onConnectionAdded = { peerId, sessionId, messagingManager ->
                            remotePeerRegistry.addConnection(
                                RemoteConnectionId(peerId, sessionId),
                                RemoteConnectionData(messagingManager)
                            )
                            eventBus.dispatch(
                                ktorServerCoroutineScope,
                                MessagingPeerConnectedEvent(peerId, sessionId)
                            )
                        }
                        this.onConnectionRemoved = { peerId, sessionId ->
                            remotePeerRegistry.removeConnection(RemoteConnectionId(peerId, sessionId))
                            eventBus.dispatch(
                                ktorServerCoroutineScope,
                                MessagingPeerDisconnectedEvent(peerId, sessionId)
                            )
                        }
                    }

                    getAllSortedByPriority<KtorServerApplicationConfigurer>().forEach {
                        it.setupModule(this)
                    }
                }

                val engineConnectorConfigs = getAll<EngineConnectorConfig>()
                val configurationPropertyLookup = get<ConfigurationPropertyLookup>()

                this.connectors.addAll(
                    engineConnectorConfigs.ifEmpty {
                        if (true) { // TODO deploymentMode == Development
                            listOf(
                                EngineConnectorBuilder().apply {
                                    host =
                                        configurationPropertyLookup.getConfigurationPropertyValue("kotlinw.serverbase.host")
                                    port =
                                        configurationPropertyLookup.getConfigurationPropertyTypedValue<Int>("kotlinw.serverbase.port")
                                }
                            )
                        } else {
                            throw ConfigurationException("No ktor server connector is defined.") // TODO link to help
                        }
                    }
                )
            }

            get<ApplicationEngineFactory<*, *>>()
                .create(environment) {
                    // TODO
                }
                .registerStartupTask(this) {
                    ktorServerCoroutineScope.launch {
                        it.start(wait = true)
                    }
                }
                .registerShutdownTask(this) {
                    it.stop()
                }
        }
    }
}
