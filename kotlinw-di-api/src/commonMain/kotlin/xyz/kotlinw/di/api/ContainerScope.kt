package xyz.kotlinw.di.api

import kotlinw.util.coroutine.SuspendingCloseable
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator

interface ContainerScope

interface ContainerScopeInternal: ContainerScope, SuspendingCloseable {

    suspend fun start()

    @ComponentQuery
    val containerLifecycleCoordinator: ContainerLifecycleCoordinator?

    @ComponentQuery
    val containerLifecycleStaticListeners: List<ContainerLifecycleListener>
}

suspend fun ContainerScope.start() = (this as ContainerScopeInternal).start()

// TODO ennek a használatait use()-zal helyettesíteni
suspend fun ContainerScope.close() = (this as ContainerScopeInternal).close()
