package kotlinw.module.core.api

import kotlin.reflect.KClass
import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.StandardJvmConfigurationPropertyResolver
import kotlinw.koin.core.api.startKoin
import kotlinw.koin.core.internal.createPidFile
import kotlinw.koin.core.internal.deletePidFile
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

@PublishedApi
internal val coreModuleLogger by lazy { PlatformLogging.getLogger() }

inline fun <reified T : Any> coreJvmModule() = coreJvmModule(T::class)

fun <T : Any> coreJvmModule(applicationClass: KClass<T>) = coreJvmModule(applicationClass.java.classLoader)

fun coreJvmModule(classLoader: ClassLoader) =
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
                StandardJvmConfigurationPropertyResolver(get(), classLoader)
            )
        }
    }

@PublishedApi
internal const val KOIN_ROOT_SCOPE_ID = "_root_"

@KoinApplicationDslMarker
inline fun <reified A : Any> runJvmApplication(
    args: Array<out String>,
    vararg modules: Module,
    noinline block: suspend Scope.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    runJvmApplication(A::class, args, modules, block)
}

@KoinApplicationDslMarker
@PublishedApi
internal fun <A : Any> runJvmApplication(
    applicationClass: KClass<A>,
    args: Array<out String>,
    modules: Array<out Module>,
    block: suspend Scope.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    createPidFile()

    val koinApplication = startKoin {
        // TODO az args legyen elérhető az alkalmazás számára
        this.modules(coreJvmModule(applicationClass.java.classLoader), *modules)
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

    try {
        runBlocking {
            koinApplication.koin.getScope(KOIN_ROOT_SCOPE_ID).block()
        }
    } finally {
        koinApplication.close()
    }
}
