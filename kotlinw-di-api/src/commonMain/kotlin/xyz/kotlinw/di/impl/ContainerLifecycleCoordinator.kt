package xyz.kotlinw.di.impl

import kotlin.concurrent.Volatile
import kotlinw.util.stdlib.Priority
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import xyz.kotlinw.di.api.ContainerLifecycleListener

interface ContainerLifecycleCoordinator {

    fun <T : Any> registerListener(
        onStartup: suspend () -> T,
        onShutdown: suspend (T) -> Unit,
        priority: Priority = Priority.Normal
    )
}

class ContainerLifecycleCoordinatorImpl(
    private val staticListeners: List<ContainerLifecycleListener>
) : ContainerLifecycleCoordinator {

    private val additionalListeners = mutableListOf<ContainerLifecycleListener>()

    private var runStartupTasksInvoked = false

    private val listenersLock = reentrantLock()

    override fun <T : Any> registerListener(
        onStartup: suspend () -> T,
        onShutdown: suspend (T) -> Unit,
        priority: Priority
    ) {
        listenersLock.withLock {
            check(!runStartupTasksInvoked)
            additionalListeners.add(
                object : ContainerLifecycleListener {

                    override val lifecycleListenerPriority get() = priority

                    @Volatile
                    private var startupResult: T? = null

                    override suspend fun onContainerStartup() {
                        startupResult = onStartup()
                    }

                    override suspend fun onContainerShutdown() {
                        onShutdown(startupResult!!)
                    }
                }
            )
        }
    }

    suspend fun runStartupTasks() {
        listenersLock.withLock {
            runStartupTasksInvoked = true
        }

        (staticListeners + additionalListeners)
            .sortedBy { it.lifecycleListenerPriority }
            .forEach {
                it.onContainerStartup() // TODO handle exceptions
            }
    }
}
