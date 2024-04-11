package xyz.kotlinw.di.api

import arrow.core.NonFatal
import arrow.core.nonFatalOrThrow
import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.delay
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl

// TODO non-public API
// TODO hibakezelés
suspend fun <S : ContainerScope, T> runApplication(
    rootScopeFactory: () -> S,
    beforeScopeCreated: () -> Unit = {},
    afterUninitializedScopeCreated: (S) -> Unit = {},
    shutdown: () -> Unit = {},
    block: suspend S.() -> T = {
        delay(Long.MAX_VALUE)
        throw IllegalStateException() // Should not be reached
    }
): T {
    beforeScopeCreated()
    val rootScope = rootScopeFactory()
    afterUninitializedScopeCreated(rootScope)

    return try {
        with(rootScope) {
            try {
                // TODO részletes hibakezelést lépésenként
                try {
                    start()
                } catch (e: Throwable) {
                    e.printStackTrace() // TODO log, tovább dobni?
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
            } finally {
                try {
                    shutdownApplication(this, containerLifecycleCoordinator)
                } catch (e: Throwable) {
                    e.nonFatalOrThrow().printStackTrace() // TODO log
                }
            }
        }
    } catch (e: Throwable) {
        if (NonFatal(e)) {
            e.printStackTrace() // TODO log
        }
        throw e
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
