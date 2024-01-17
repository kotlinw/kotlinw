package xyz.kotlinw.module.ktor.server

import io.ktor.server.application.applicationProperties
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngine.Configuration
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.getConfigurationPropertyTypedValue
import kotlinw.configuration.core.getConfigurationPropertyValue
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.util.coroutine.createNestedSupervisorScope
import kotlinw.util.stdlib.Priority
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ContainerLifecycleListener
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.module.core.ApplicationCoroutineService

@Component
class KtorApplicationEngineManager(
    private val loggerFactory: LoggerFactory,
    private val applicationCoroutineService: ApplicationCoroutineService,
    private val configurers: List<KtorServerApplicationConfigurer>,
    private val engineConnectorConfigs: List<EngineConnectorConfig>,
    private val configurationPropertyLookup: ConfigurationPropertyLookup,
    private val applicationEngineFactory: ApplicationEngineFactory<*, *>,
    private val deploymentMode: DeploymentMode
) : ContainerLifecycleListener {

    private var embeddedServerHolder by atomic<EmbeddedServer<ApplicationEngine, out Configuration>?>(null)

    override val lifecycleListenerPriority: Priority get() = Priority.Normal

    override suspend fun onContainerStartup() {
        // See the `private`: PlatformUtilsJvm.DEVELOPMENT_MODE_KEY
        System.setProperty("io.ktor.development", (deploymentMode == DeploymentMode.Development).toString())

        val logger = loggerFactory.getLogger()

        KtorServerModule.initializeKtorServerApplicationConfigurers(configurers)

        val ktorServerCoroutineScope =
            applicationCoroutineService.applicationCoroutineScope.createNestedSupervisorScope()

        // TODO set development mode
        val embeddedServer = embeddedServer(
            factory = applicationEngineFactory,
            applicationProperties = applicationProperties {
                this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext
                applicationEnvironment {
                    log = KtorSimpleLogger(logger.name)
                }
                module {
                    KtorServerModule.initializeKtorServerApplication(
                        this,
                        deploymentMode,
                        ktorServerCoroutineScope,
                        configurers
                    )
                }
            },
            configure = {
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
        )
        embeddedServerHolder = embeddedServer

        ktorServerCoroutineScope.launch {
            embeddedServer.start(wait = true)
            // TODO log
        }
    }

    override suspend fun onContainerShutdown() {
        embeddedServerHolder?.stop()
    }
}
