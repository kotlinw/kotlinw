package xyz.kotlinw.di.api

import arrow.core.nonFatalOrThrow
import kotlinw.util.coroutine.runCatchingNonCancellableCleanup
import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.delay
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl
import xyz.kotlinw.util.stdlib.runCatchingCleanup

// TODO hibakezelés
@DelicateKotlinwApi
suspend fun <S : ContainerScope, T> runApplication(
    rootScopeFactory: () -> S,
    beforeScopeCreated: () -> Unit = {},
    afterUninitializedScopeCreated: (S) -> Unit = {},
    afterScopeStarted: (S) -> Unit = {},
    startupTaskFilter: (String) -> Boolean = { true },
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
        with(rootScope as ContainerScopeInternal) {
            try {
                // TODO részletes hibakezelést lépésenként
                try {
                    start()
                } catch (e: Throwable) {
                    throw RuntimeException("Application start failed.", e.nonFatalOrThrow())
                }

                val coordinator = containerLifecycleCoordinator as ContainerLifecycleCoordinatorImpl?

                if (coordinator != null) {
                    containerLifecycleStaticListeners.forEach { listener ->
                        coordinator.registerListener(
                            listener.getLifecycleListenerId(),
                            listener::onContainerStartup,
                            { listener.onContainerShutdown() },
                            listener.lifecycleListenerPriority
                        )
                    }
                }

                afterScopeStarted(rootScope)

                coordinator?.runStartupTasks(startupTaskFilter)

                block(rootScope)
            } finally {
                shutdownApplication(this)
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
suspend fun shutdownApplication(containerScope: ContainerScope) {
    check(containerScope is ContainerScopeInternal)

    runCatchingNonCancellableCleanup {
        (containerScope.containerLifecycleCoordinator as? ContainerLifecycleCoordinatorImpl)?.runShutdownTasks()
    }
    runCatchingNonCancellableCleanup {
        containerScope.close()
    }
}
