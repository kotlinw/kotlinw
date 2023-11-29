package xyz.kotlinw.di.api

import kotlinx.coroutines.delay
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl

// TODO non-public API
// TODO hibakezelés
suspend fun <T : ContainerScope> runApplication(
    rootScopeFactory: () -> T,
    beforeScopeCreated: () -> Unit = {},
    afterUninitializedScopeCreated: (T) -> Unit = {},
    shutdown: () -> Unit = {},
    block: suspend T.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    beforeScopeCreated()
    val rootScope = rootScopeFactory()
    afterUninitializedScopeCreated(rootScope)

    try {
        with(rootScope) {
            try {
                // TODO részletes hibakezelést lépésenként
                start()
                (containerLifecycleCoordinator as? ContainerLifecycleCoordinatorImpl)?.runStartupTasks()

                block()
            } catch (e: Exception) {
                e.printStackTrace() // TODO log
            } finally {
                close()
            }
        }
    } finally {
        shutdown()
    }
}
