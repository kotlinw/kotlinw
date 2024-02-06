package xyz.kotlinw.di.api

import arrow.core.nonFatalOrThrow
import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.delay
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
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
                try {
                    start()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                containerLifecycleCoordinator?.apply {
                    check(this is ContainerLifecycleCoordinatorImpl)

                    containerLifecycleStaticListeners.forEach { listener ->
                        registerListener(
                            listener::onContainerStartup,
                            { listener.onContainerShutdown() },
                            listener.lifecycleListenerPriority
                        )
                    }

                    // TODO nem szép, hogy az implementációtól függünk
                    (containerLifecycleCoordinator as? ContainerLifecycleCoordinatorImpl)?.runStartupTasks()
                }

                block()
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException(e.nonFatalOrThrow()) // TODO log
            } finally {
                try {
                    shutdownApplication(this, containerLifecycleCoordinator)
                } catch (e: Throwable) {
                    e.nonFatalOrThrow().printStackTrace() // TODO log
                }
            }
        }
    } catch (e: Throwable) {
        e.nonFatalOrThrow().printStackTrace() // TODO
    } finally {
        shutdown()
    }
}

@DelicateKotlinwApi
suspend fun shutdownApplication(
    containerScope: ContainerScope,
    containerLifecycleCoordinator: ContainerLifecycleCoordinator?
) {
    (containerLifecycleCoordinator as? ContainerLifecycleCoordinatorImpl)?.runShutdownTasks()
    containerScope.close()
}
