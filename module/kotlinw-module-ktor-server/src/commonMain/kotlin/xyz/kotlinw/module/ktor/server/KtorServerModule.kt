package xyz.kotlinw.module.ktor.server

import io.ktor.http.CacheControl.NoCache
import io.ktor.http.CacheControl.NoStore
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.getConfigurationPropertyTypedValue
import kotlinw.configuration.core.getConfigurationPropertyValue
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.util.coroutine.createNestedSupervisorScope
import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.module.core.ApplicationCoroutineService
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer.Context

@Module
class KtorServerModule {

    companion object {

        @DelicateKotlinwApi
        suspend fun initializeKtorServerApplicationConfigurers(configurers: List<KtorServerApplicationConfigurer>) {
            configurers.forEach {
                try {
                    it.initialize()
                } catch (e: Exception) {
                    throw RuntimeException("Failed to initialize Ktor server configurer: " + it, e)
                }
            }
        }

        @DelicateKotlinwApi
        fun initializeKtorServerApplication(
            ktorApplication: Application,
            deploymentMode: DeploymentMode,
            ktorServerCoroutineScope: CoroutineScope,
            configurers: List<KtorServerApplicationConfigurer>
        ) {
            with(ktorApplication) {
                install(CachingHeaders) {
                    // TODO https://youtrack.jetbrains.com/issue/KTOR-750/Setting-multiple-cache-control-directives-is-impossible-with-current-API
                    options { _, _ -> CachingOptions(NoCache(null)) }
                    options { _, _ -> CachingOptions(NoStore(null)) }
                }

                val context = Context(this, ktorServerCoroutineScope)
                configurers.sortedBy { it.priority }.forEach {
                    it.setupKtorModule(context)
                }
            }
        }
    }

    @Component
    fun ktorApplicationEngineManager(
        loggerFactory: LoggerFactory,
        applicationCoroutineService: ApplicationCoroutineService,
        configurers: List<KtorServerApplicationConfigurer>,
        engineConnectorConfigs: List<EngineConnectorConfig>,
        configurationPropertyLookup: ConfigurationPropertyLookup,
        applicationEngineFactory: ApplicationEngineFactory<*, *>,
        deploymentMode: DeploymentMode,
        lifecycleCoordinator: ContainerLifecycleCoordinator
    ) {
        lifecycleCoordinator.registerListener(
            {
                val logger = loggerFactory.getLogger()

                initializeKtorServerApplicationConfigurers(configurers)

                val ktorServerCoroutineScope =
                    applicationCoroutineService.applicationCoroutineScope.createNestedSupervisorScope()

                val environment = applicationEngineEnvironment {
                    this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext

                    this.log = KtorSimpleLogger(logger.name)
                    // TODO valamiért nem tölti újra a ktor: this.developmentMode = get<DeploymentMode>() == Development

                    this.module {
                        initializeKtorServerApplication(
                            this@module,
                            deploymentMode,
                            ktorServerCoroutineScope,
                            configurers
                        )
                    }

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

                val applicationEngine = applicationEngineFactory.create(environment) {
                    // TODO
                }

                ktorServerCoroutineScope.launch {
                    applicationEngine.start(wait = true)
                    // TODO log
                }
            },
            {}
        )
    }
}
