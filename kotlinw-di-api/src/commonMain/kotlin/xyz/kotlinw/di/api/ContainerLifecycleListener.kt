package xyz.kotlinw.di.api

import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.debugName

// TODO megkülönböztetni a Listener-t és a ListenerRegistration-t, az utóbbiban ne legyenek default metódusok, az előbbi pedig lehet egy abstract class, amiben a priority-nek is van default értéke
interface ContainerLifecycleListener {

    val lifecycleListenerPriority: Priority

    suspend fun onContainerStartup() {}

    suspend fun onContainerShutdown() {}

    fun getLifecycleListenerId(): String = this::class.debugName // TODO erre alkalmas lenne a ComponentId?
}
