package kotlinw.module.core.api

import kotlin.reflect.KClass
import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.StandardJvmConfigurationPropertyResolver
import kotlinw.koin.core.api.coreModuleLogger
import kotlinw.koin.core.api.startContainer
import kotlinw.koin.core.internal.createPidFile
import kotlinw.koin.core.internal.deletePidFile
import kotlinx.coroutines.delay
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module
import xyz.kotlinw.koin.container.KOIN_ROOT_SCOPE_ID

// TODO ez miért nem lehet internal?
inline fun <reified T : Any> coreJvmModule() = coreJvmModule(T::class)

fun <T : Any> coreJvmModule(applicationClass: KClass<T>) = coreJvmModule(applicationClass.java.classLoader)

// TODO induláskor:
// SLF4J: A number (2) of logging calls during the initialization phase have been intercepted and are
// SLF4J: now being replayed. These are subject to the filtering rules of the underlying logging system.
// SLF4J: See also https://www.slf4j.org/codes.html#replay

// TODO deploymentMode jöhetne paraméterben?
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
                StandardJvmConfigurationPropertyResolver(get(), get(), classLoader)
            )
        }
    }

@KoinApplicationDslMarker
suspend inline fun <reified A : Any> runJvmApplication(
    args: Array<out String>,
    vararg modules: Module,
    noinline block: suspend Scope.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    runJvmApplication(A::class, args, modules, block)
}

@KoinApplicationDslMarker
@PublishedApi
internal suspend fun <A : Any> runJvmApplication(
    applicationClass: KClass<A>,
    args: Array<out String>,
    modules: Array<out Module>,
    block: suspend Scope.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    createPidFile()

    val koinApplication = startContainer(
        appDeclaration = {
            // TODO az args legyen elérhető az alkalmazás számára
            this.modules(coreJvmModule(applicationClass.java.classLoader), *modules)
        },
        onUninitializedKoinApplicationInstanceCreated = {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    try {
                        it.close()
                    } finally {
                        deletePidFile()
                    }
                }
            )
        }
    )

    try {
        koinApplication.koin.getScope(KOIN_ROOT_SCOPE_ID).block()
    } finally {
        koinApplication.close()
    }
}
