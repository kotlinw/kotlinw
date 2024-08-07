package xyz.kotlinw.module.core

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import xyz.kotlinw.eventbus.inprocess.LocalEvent
import xyz.kotlinw.module.configuration.ConfigurationModule
import xyz.kotlinw.module.logging.LoggingModule
import xyz.kotlinw.module.serializer.SerializerModule

// TODO a containerLifecycleCoordinator komponensre fix hivatkozás van a DoSymbolProcessor-ban!
@Module(includeModules = [SerializerModule::class, LoggingModule::class, ConfigurationModule::class])
class CoreModule {

    @Component(type = ContainerLifecycleCoordinator::class)
    fun containerLifecycleCoordinator() = ContainerLifecycleCoordinatorImpl()

    @Component(type = ApplicationCoroutineService::class, onTerminate = "close")
    fun applicationCoroutineService() = ApplicationCoroutineServiceImpl()

    @Component
    fun applicationEventBus(): InProcessEventBus<LocalEvent> = InProcessEventBus()
}
