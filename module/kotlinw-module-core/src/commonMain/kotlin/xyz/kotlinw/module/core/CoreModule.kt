package xyz.kotlinw.module.core

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ContainerLifecycleListener
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import xyz.kotlinw.module.configuration.ConfigurationModule
import xyz.kotlinw.module.logging.LoggingModule
import xyz.kotlinw.module.serializer.SerializerModule

@Module(includeModules = [SerializerModule::class, LoggingModule::class, ConfigurationModule::class])
class CoreModule {

    @Component(type = ContainerLifecycleCoordinator::class)
    fun containerLifecycleCoordinator(listeners: List<ContainerLifecycleListener>) =
        ContainerLifecycleCoordinatorImpl(listeners)

    @Component(type = ApplicationCoroutineService::class, onTerminate = "close")
    fun applicationCoroutineService() = ApplicationCoroutineServiceImpl()

    @Component
    fun localEventBus(): InProcessEventBus = InProcessEventBus()
}