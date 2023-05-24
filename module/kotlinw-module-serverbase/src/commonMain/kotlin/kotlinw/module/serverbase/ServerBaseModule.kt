package kotlinw.module.serverbase

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.koin.core.api.coreKoinModule
import kotlinw.koin.core.api.getAllSortedByPriority
import kotlinw.koin.core.api.registerOnShutdownTask
import kotlinw.koin.core.api.registerOnStartupTask
import kotlinw.module.core.api.ApplicationCoroutineService
import kotlinw.module.core.api.createNestedSupervisorScope
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import org.koin.dsl.module

inline fun <reified TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> serverBaseModule(
    factory: ApplicationEngineFactory<TEngine, TConfiguration>,
    crossinline configure: TConfiguration.(Scope) -> Unit = {} //TODO context(Scope)
) = module {
    single<TEngine>(createdAtStart = true) {
        val ktorServerCoroutineScope = get<ApplicationCoroutineService>().coroutineScope.createNestedSupervisorScope()

        val environment = applicationEngineEnvironment {
            this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext

            this.log = KtorSimpleLogger("ktor.application") // TODO

            this.module {
                getAllSortedByPriority<KtorServerApplicationModule>().forEach {
                    it.setupModule(this)
                }
            }

            val engineConnectorConfigs = buildList {
                addAll(getAll<EngineConnectorConfig>())
            }

            this.connectors.addAll(
                engineConnectorConfigs.ifEmpty {
                    if (true) { // TODO deploymentMode == Development
                        listOf(
                            EngineConnectorBuilder().apply {
                                host = "localhost"
                                port = 8080
                            }
                        )
                    } else {
                        throw ConfigurationException("No ktor server connector is defined.") // TODO link to help
                    }
                }
            )
        }

        factory
            .create(environment) {
                configure(this, this@single)
            }
            .registerOnStartupTask(this) {
                ktorServerCoroutineScope.launch {
                    it.start(wait = true)
                }
            }
            .registerOnShutdownTask(this) {
                it.stop()
            }
    }
}
