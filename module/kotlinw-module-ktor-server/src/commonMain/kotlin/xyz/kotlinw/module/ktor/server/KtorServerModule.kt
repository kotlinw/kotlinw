package xyz.kotlinw.module.ktor.server

import io.ktor.http.CacheControl.NoCache
import io.ktor.http.CacheControl.NoStore
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.DeploymentMode.Development
import kotlinw.configuration.core.getConfigurationPropertyTypedValue
import kotlinw.configuration.core.getConfigurationPropertyValue
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.util.coroutine.createNestedSupervisorScope
import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.event.Level.DEBUG
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer.Context

@Module
class KtorServerModule {

    companion object {

        @DelicateKotlinwApi
        fun initializeKtorServerApplication(
            ktorApplication: Application,
            deploymentMode: DeploymentMode,
            ktorServerCoroutineScope: CoroutineScope,
            configurers: List<KtorServerApplicationConfigurer>
        ) {
            with(ktorApplication) {
                if (deploymentMode == Development) {
                    install(CallLogging) {
                        level = DEBUG
                    }
                }

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
    fun ktorApplicationEngine(
        loggerFactory: LoggerFactory,
        applicationCoroutineService: ApplicationCoroutineService,
        configurers: List<KtorServerApplicationConfigurer>,
        engineConnectorConfigs: List<EngineConnectorConfig>,
        configurationPropertyLookup: ConfigurationPropertyLookup,
        applicationEngineFactory: ApplicationEngineFactory<*, *>,
        deploymentMode: DeploymentMode
    ): ApplicationEngine {
        val ktorServerCoroutineScope = applicationCoroutineService.coroutineScope.createNestedSupervisorScope()

        val environment = applicationEngineEnvironment {
            this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext

            val logger = loggerFactory.getLogger()
            this.log = KtorSimpleLogger(logger.name)
            // TODO valamiért nem tölti újra a ktor: this.developmentMode = get<DeploymentMode>() == Development

            this.module {
                initializeKtorServerApplication(this@module, deploymentMode, ktorServerCoroutineScope, configurers)
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

        return applicationEngine // TODO shutdown
    }
}
