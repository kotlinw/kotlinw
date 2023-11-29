package xyz.kotlinw.di.api

import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator

interface ContainerScope {

    suspend fun start()

    suspend fun close()

    // TODO this should not be here (it is here only because it made the implementation easy)
    @ComponentQuery
    val containerLifecycleCoordinator: ContainerLifecycleCoordinator?
}
