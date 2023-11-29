package xyz.kotlinw.di.impl

import kotlinw.util.stdlib.Priority
import xyz.kotlinw.di.api.ContainerLifecycleListener

interface ContainerLifecycleCoordinator {

    fun registerListener(onStartup: suspend () -> Unit, onShutdown: suspend () -> Unit, priority: Priority = Priority.Normal)
}

class ContainerLifecycleCoordinatorImpl(
    private val staticListeners: List<ContainerLifecycleListener>
) : ContainerLifecycleCoordinator {

    private val additionalListeners = mutableListOf<ContainerLifecycleListener>()

    override fun registerListener(onStartup: suspend () -> Unit, onShutdown: suspend () -> Unit, priority: Priority) {
        additionalListeners.add(
            object : ContainerLifecycleListener {

                override val lifecycleListenerPriority get() = priority

                override suspend fun onContainerStartup() {
                    onStartup()
                }

                override suspend fun onContainerShutdown() {
                    onShutdown()
                }
            }
        )
    }

    suspend fun runStartupTasks() {
        // TODO ezután már ne lehessen újat hozzáadni az additionalListeners-hez
        (staticListeners + additionalListeners).sortedBy { it.lifecycleListenerPriority }.forEach {
            it.onContainerStartup() // TODO handle exceptions
        }
    }
}
