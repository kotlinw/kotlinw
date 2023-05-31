package kotlinw.module.serverbase

import io.ktor.server.application.*
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.getConfigurationPropertyTypedValue
import kotlinw.configuration.core.getConfigurationPropertyTypedValueOrNull
import kotlinw.configuration.core.getConfigurationPropertyValue
import kotlinw.configuration.core.getConfigurationPropertyValueOrNull
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.koin.core.api.getAllSortedByPriority
import kotlinw.koin.core.api.registerShutdownTask
import kotlinw.koin.core.api.registerStartupTask
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.server.ktor.RemotingPlugin
import kotlinw.util.coroutine.createNestedSupervisorScope
import kotlinx.coroutines.launch
import org.koin.dsl.module

val serverBaseModule by lazy {
    module {
        single<ApplicationEngine>(createdAtStart = true) {
            val ktorServerCoroutineScope =
                get<ApplicationCoroutineService>().coroutineScope.createNestedSupervisorScope()

            val environment = applicationEngineEnvironment {
                this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext

                this.log = KtorSimpleLogger("kotlinw.serverbase.ktor")

                this.module {
                    install(RemotingPlugin) {
                        this.messageCodec = get<MessageCodec<*>>()
                        this.remoteCallDelegators = getAll<RemoteCallDelegator>()
                        this.identifyClient = { 1 } // TODO
                    }

                    getAllSortedByPriority<KtorServerApplicationConfigurer>().forEach {
                        it.setupModule(this)
                    }
                }

                val engineConnectorConfigs = buildList {
                    addAll(getAll<EngineConnectorConfig>())
                }

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
