package kotlinw.module.serverbase

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineConnectorConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.util.logging.KtorSimpleLogger
import kotlinw.configuration.core.ConfigurationException
import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.koin.core.api.getAllSortedByPriority
import kotlinw.koin.core.api.registerShutdownTask
import kotlinw.koin.core.api.registerStartupTask
import kotlinw.util.coroutine.createNestedSupervisorScope
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

val  serverBaseModule by lazy {
    module {
        single<ApplicationEngine>(createdAtStart = true) {
            val ktorServerCoroutineScope = get<ApplicationCoroutineService>().coroutineScope.createNestedSupervisorScope()

            val environment = applicationEngineEnvironment {
                this.parentCoroutineContext = ktorServerCoroutineScope.coroutineContext

                this.log = KtorSimpleLogger("ktor.application") // TODO

                this.module {
                    getAllSortedByPriority<KtorServerApplicationConfigurer>().forEach {
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

            get<ApplicationEngineFactory<*,*>>()
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
