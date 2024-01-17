package xyz.kotlinw.module.ktor.server

import io.ktor.http.CacheControl.NoCache
import io.ktor.http.CacheControl.NoStore
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import kotlinw.configuration.core.DeploymentMode
import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.CoroutineScope
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer.Context

@Module
@ComponentScan
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
}
