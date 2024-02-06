package xyz.kotlinw.di.api

import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator

// TODO ezeknek a metódusoknak valami rejtett helyen kellene lennie, mert ha a ContainerScope receiver-ként van használva, akkor simán meg lehet hívni őket bármikor
interface ContainerScope {

    suspend fun start()

    suspend fun close()

    // TODO this should not be here (it is here only because it made the implementation easy, it should be refactored)
    @ComponentQuery
    val containerLifecycleCoordinator: ContainerLifecycleCoordinator?

    // TODO this should not be here (it is here only because it made the implementation easy, it should be refactored)
    @ComponentQuery
    val containerLifecycleStaticListeners: List<ContainerLifecycleListener>
}
