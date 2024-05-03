package xyz.kotlinw.di.api

import arrow.core.nonFatalOrThrow
import kotlinw.util.coroutine.runCatchingNonCancellableCleanup
import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.delay
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl
import xyz.kotlinw.util.stdlib.runCatchingCleanup

// TODO non-public API
// TODO hibakezelés
suspend fun <S : ContainerScope, T> runApplication(
    rootScopeFactory: () -> S,
    beforeScopeCreated: () -> Unit = {},
    afterUninitializedScopeCreated: (S) -> Unit = {},
    onShutdown: () -> Unit = {},
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
                    throw RuntimeException("Application start failed.", e.nonFatalOrThrow())
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
                shutdownApplication(this, containerLifecycleCoordinator)
            }
        }
    } catch (e: Throwable) {
        e.nonFatalOrThrow().printStackTrace() // TODO log
        throw e
    } finally {
        runCatchingCleanup {
            onShutdown()
        }
    }
}

@DelicateKotlinwApi
suspend fun shutdownApplication(
    containerScope: ContainerScope,
    containerLifecycleCoordinator: ContainerLifecycleCoordinator?
) {
    runCatchingNonCancellableCleanup {
        (containerLifecycleCoordinator as? ContainerLifecycleCoordinatorImpl)?.runShutdownTasks()
    }
    runCatchingNonCancellableCleanup {
        containerScope.close()
    }
}
