@file:JvmName("ContainerUtilJvm")

package kotlinw.koin.core.api

import kotlinw.koin.core.internal.ContainerShutdownCoordinator
import kotlinw.koin.core.internal.ContainerStartupCoordinator
import kotlinw.koin.core.internal.OnShutdownTask
import kotlinw.koin.core.internal.OnStartupTask
import kotlinw.module.api.ApplicationInitializerService
import kotlinw.util.stdlib.sortedByPriority
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.scope.Scope
import org.koin.dsl.KoinAppDeclaration
import kotlin.jvm.JvmName
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@PublishedApi
internal const val KOIN_ROOT_SCOPE_ID = "_root_"

@PublishedApi
internal val coreModuleLogger by lazy { PlatformLogging.getLogger() }

inline fun <reified T : Any> Scope.getAllSortedByPriority(): List<T> =
    getAll<T>(T::class).sortedByPriority()

inline fun <reified T : Any> Koin.getAllSortedByPriority(): List<T> =
    getAll<T>().sortedByPriority()

// TODO context(Scope)
fun <T : Any> T.registerStartupTask(scope: Scope, onStartupTask: OnStartupTask<T>): T {
    scope.get<ContainerStartupCoordinator>().registerOnStartupTask(this, onStartupTask)
    return this
}

// TODO context(Scope)
fun <T : Any> T.registerShutdownTask(scope: Scope, onShutdownTask: OnShutdownTask<T>): T {
    scope.get<ContainerShutdownCoordinator>().registerOnShutdownTask(this, onShutdownTask)
    return this
}

@KoinApplicationDslMarker
suspend fun startContainer(
    appDeclaration: KoinAppDeclaration,
    onUninitializedKoinApplicationInstanceCreated: (KoinApplication) -> Unit = {}
): KoinApplication {
    val koinApplication = coroutineScope {
        org.koin.core.context.startKoin {
            allowOverride(false)
            onUninitializedKoinApplicationInstanceCreated(this)

            modules(coreModule)
            appDeclaration()

            launch {
                koin.getAllSortedByPriority<ApplicationInitializerService>().forEach {
                    try {
                        it.performInitialization()
                    } catch (e: Exception) {
                        // TODO log
                        e.printStackTrace()
                        throw e
                    }
                }
            }
        }
    }

    return koinApplication
}
