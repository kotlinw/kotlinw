package kotlinw.module.serverbase

import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.DeploymentMode.Development
import kotlinw.configuration.core.getConfigurationPropertyTypedValue
import kotlinw.configuration.core.getConfigurationPropertyValue
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.dispatch
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.koin.core.api.getAllSortedByPriority
import kotlinw.koin.core.api.registerShutdownTask
import kotlinw.koin.core.api.registerStartupTask
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.RemoteConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.remoting.server.ktor.RemotingServerPlugin
import kotlinw.remoting.server.ktor.ServerToClientCommunicationType
import kotlinw.util.coroutine.createNestedSupervisorScope
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import org.koin.dsl.module

val serverRemotingModule by lazy {
    module {
        includes(serverBaseModule)

        single<KtorServerApplicationConfigurer>(named("serverRemotingModule.setupRemoting")) {
            KtorServerApplicationConfigurer(Priority.Normal.lowerBy(10)) {
                val ktorApplication = application

                val remoteCallDelegators = getAll<RemoteCallDelegator>()
                if (remoteCallDelegators.isNotEmpty()) {
                    ktorApplication.install(RemotingServerPlugin) {
                        val eventBus = get<LocalEventBus>()
                        val remotePeerRegistry = get<MutableRemotePeerRegistry>()

                        this.messageCodec = get<MessageCodec<*>>()
                        this.remoteCallDelegators = remoteCallDelegators
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
                }
            }
        }
    }
}

private class ApplicationEngineWrapper : ApplicationEngine {

    private val wrappedHolder = atomic<ApplicationEngine?>(null)

    fun setWrapped(applicationEngine: ApplicationEngine) {
        check(wrappedHolder.value == null)
        wrappedHolder.value = applicationEngine
    }

    private fun wrapped() = wrappedHolder.value ?: throw IllegalStateException()

    override val environment: ApplicationEngineEnvironment get() = wrapped().environment

    override suspend fun resolvedConnectors(): List<EngineConnectorConfig> = wrapped().resolvedConnectors()

    override fun start(wait: Boolean): ApplicationEngine = wrapped().start(wait)

    override fun stop(gracePeriodMillis: Long, timeoutMillis: Long) =
        wrapped().stop(gracePeriodMillis, timeoutMillis)
}

val serverBaseModule by lazy {
    module {
        single<ApplicationEngine>(createdAtStart = true) {
            ApplicationEngineWrapper()
                .registerStartupTask(this) { applicationEngineWrapper ->
                    val logger = get<LoggerFactory>().getLogger()

                    val ktorServerCoroutineScope =
                        get<ApplicationCoroutineService>().coroutineScope.createNestedSupervisorScope()

                    val environment = applicationEngineEnvironment {
                        this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext
                        this.log = KtorSimpleLogger(logger.name)
                        // TODO valamiért nem tölti újra: this.developmentMode = get<DeploymentMode>() == Development

                        this.module {
                            val context = KtorServerApplicationConfigurer.Context(this, ktorServerCoroutineScope)
                            getAllSortedByPriority<KtorServerApplicationConfigurer>().forEach {
                                it.setupModule(context)
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

                    applicationEngineWrapper.setWrapped(
                        get<ApplicationEngineFactory<*, *>>()
                            .create(environment) {
                                // TODO
                            }
                    )

                    ktorServerCoroutineScope.launch {
                        applicationEngineWrapper.start(wait = true)
                    }
                }
                .registerShutdownTask(this) {
                    it.stop()
                }
        }
    }
}
