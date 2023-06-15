package kotlinw.module.core.api

import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.StandardJvmConfigurationPropertyResolver
import kotlinw.koin.core.api.startKoin
import kotlinw.koin.core.internal.createPidFile
import kotlinw.koin.core.internal.deletePidFile
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.dsl.module

@PublishedApi
internal val coreModuleLogger by lazy { PlatformLogging.getLogger() }

inline fun <reified T> T.coreJvmModule() =
    module {
        single {
            val deploymentModeFromSystemProperty = System.getProperty("kotlinw.core.deploymentMode")
            if (deploymentModeFromSystemProperty.isNullOrBlank()) {
                DeploymentMode.Development
            } else {
                DeploymentMode.of(deploymentModeFromSystemProperty)
            }.also {
                coreModuleLogger.info { "Deployment mode: " / it }
            }
        }

        single<ConfigurationPropertyLookupSource> {
            EnumerableConfigurationPropertyLookupSourceImpl(
                StandardJvmConfigurationPropertyResolver(get(), T::class.java.classLoader)
            )
        }
    }

@KoinApplicationDslMarker
inline fun <reified T> runApplication(args: Array<out String> = emptyArray(), vararg modules: Module) {
    createPidFile()

    val koinApplication = startKoin {
        this.modules(coreJvmModule(), *modules)
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            try {
                koinApplication.close()
            } finally {
                deletePidFile()
            }
        }
    )

    // TODO
    runCatching {
        Thread.sleep(Long.MAX_VALUE)
    }
}
