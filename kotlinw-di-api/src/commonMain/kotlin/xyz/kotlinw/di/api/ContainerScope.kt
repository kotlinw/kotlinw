package xyz.kotlinw.di.api

import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator

interface ContainerScope {

    // TODO this should not be here (it is here only because it made the implementation easy, it should be refactored)
    suspend fun start()

    // TODO this should not be here (it is here only because it made the implementation easy, it should be refactored)
    suspend fun close()

    // TODO this should not be here (it is here only because it made the implementation easy, it should be refactored)
    @ComponentQuery
    val containerLifecycleCoordinator: ContainerLifecycleCoordinator?

    // TODO this should not be here (it is here only because it made the implementation easy, it should be refactored)
    @ComponentQuery
    val containerLifecycleStaticListeners: List<ContainerLifecycleListener>
}
