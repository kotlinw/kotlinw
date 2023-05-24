package kotlinw.koin.core.api

import kotlinw.koin.core.internal.ContainerShutdownCoordinator
import kotlinw.koin.core.internal.ContainerStartupCoordinator
import kotlinw.koin.core.internal.OnShutdownTask
import kotlinw.koin.core.internal.OnStartupTask
import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority
import org.koin.core.KoinApplication
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.scope.Scope
import org.koin.dsl.KoinAppDeclaration

inline fun <reified T : Any> Scope.getAllSortedByPriority(): List<T> =
    getAll<T>(T::class).sortedBy {
        if (it is HasPriority) {
            it.priority
        } else {
            Priority.Normal
        }
    }

// TODO context(Scope)
fun <T : Any> T.registerOnStartupTask(scope: Scope, onStartupTask: OnStartupTask<T>): T {
    scope.get<ContainerStartupCoordinator>().registerOnStartupTask(this, onStartupTask)
    return this
}

// TODO context(Scope)
fun <T : Any> T.registerOnShutdownTask(scope: Scope, onShutdownTask: OnShutdownTask<T>): T {
    scope.get<ContainerShutdownCoordinator>().registerOnShutdownTask(this, onShutdownTask)
    return this
}

@KoinApplicationDslMarker
fun startKoin(appDeclaration: KoinAppDeclaration): KoinApplication =
    org.koin.core.context.startKoin {
        modules(coreModule())
        appDeclaration()
    }.apply {
        koin.get<ContainerStartupCoordinator>().runStartupTasks()
    }
