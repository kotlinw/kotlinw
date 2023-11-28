package xyz.kotlinw.koin.core.api.internal

import kotlinw.koin.core.api.ContainerLifecycleListener
import xyz.kotlinw.di.api.OnConstruction

interface ContainerLifecycleCoordinator {
}

class ContainerLifecycleCoordinatorImpl(
    private val listeners: List<ContainerLifecycleListener>
) : ContainerLifecycleCoordinator {

    @OnConstruction
    suspend fun onConstruction() {
        listeners.sortedBy { it.lifecycleListenerPriority }.forEach {
            it.onContainerStartup() // TODO handle exceptions
        }
    }
}
