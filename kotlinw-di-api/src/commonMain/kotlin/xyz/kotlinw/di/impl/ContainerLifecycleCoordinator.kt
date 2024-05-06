package xyz.kotlinw.di.impl

import arrow.core.nonFatalOrThrow
import kotlin.concurrent.Volatile
import kotlinw.collection.LinkedQueue
import kotlinw.collection.MutableQueue
import kotlinw.util.stdlib.Priority
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import xyz.kotlinw.di.api.ContainerLifecycleListener

interface ContainerLifecycleCoordinator {

    fun <T : Any> registerListener(
        onStartup: suspend () -> T,
        onShutdown: suspend (T) -> Unit,
        priority: Priority = Priority.Normal
    )

    fun initiateShutdown()
}

interface ContainerLifecycleCoordinatorInternal {

    suspend fun runStartupTasks()
}

class ContainerLifecycleCoordinatorImpl : ContainerLifecycleCoordinator, ContainerLifecycleCoordinatorInternal {

    private val listeners = mutableListOf<ContainerLifecycleListener>()

    private val runStartupTasksInvoked = atomic(false)

    private var shutdownTasks: MutableQueue<ContainerLifecycleListener> = LinkedQueue()

    private val runShutdownTasksInvoked = atomic(false)

    private val listenersLock = reentrantLock()

    override fun <T : Any> registerListener(
        onStartup: suspend () -> T,
        onShutdown: suspend (T) -> Unit,
        priority: Priority
    ) {
        listenersLock.withLock {
            // TODO it could be possible to run the startup listener immediately
            check(!runStartupTasksInvoked.value) { "Container lifecycle listener registration is not allowed because container bootstrap is already completed." }

            listeners.add(
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

    override suspend fun runStartupTasks() {
        listenersLock.withLock {
            if (!runStartupTasksInvoked.compareAndSet(false, true)) {
                throw IllegalStateException("runStartupTasks() has already been called.")
            }
        }

        try {
            val startupTasks = listeners.sortedBy { it.lifecycleListenerPriority }
            // TODO kilogolni startupTasks-ot

            startupTasks.forEach { listener ->
                try {
                    listener.onContainerStartup()
                    shutdownTasks.add(listener)
                } catch (e: Throwable) {
                    e.nonFatalOrThrow().also {
                        // TODO log
                        runShutdownTasks()
                        throw RuntimeException("Startup task failed: $listener", it)
                    }
                }
            }
        } finally {
            listeners.clear()
        }
    }

    suspend fun runShutdownTasks() {
        check(runStartupTasksInvoked.value)
        if (runShutdownTasksInvoked.compareAndSet(false, true)) {
            try {
                shutdownTasks.forEach {
                    try {
                        withContext(NonCancellable) {
                            it.onContainerShutdown()
                        }
                    } catch (e: Throwable) {
                        // FIXME itt ne dobjunk tovább semmit, mert akkor a teljes shutdown folyamat megszakad
                        e.nonFatalOrThrow() // TODO log
                    }
                }
            } finally {
                shutdownTasks.clear()
            }
        }
    }

    override fun initiateShutdown() {
        TODO()
    }
}
