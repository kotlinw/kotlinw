package xyz.kotlinw.di.impl

import arrow.core.nonFatalOrThrow
import kotlin.concurrent.Volatile
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.collection.emptyImmutableList
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.collections.immutable.toImmutableList
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

class ContainerLifecycleCoordinatorImpl : ContainerLifecycleCoordinator {

    private val listeners = mutableListOf<ContainerLifecycleListener>()

    private var runStartupTasksInvoked by atomic(false)

    private var shutdownTasks by atomic(emptyImmutableList<ContainerLifecycleListener>())

    private val runShutdownTasksInvoked = atomic(false)

    private val listenersLock = reentrantLock()

    override fun <T : Any> registerListener(
        onStartup: suspend () -> T,
        onShutdown: suspend (T) -> Unit,
        priority: Priority
    ) {
        listenersLock.withLock {
            check(!runStartupTasksInvoked)
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

    suspend fun runStartupTasks() {
        listenersLock.withLock {
            runStartupTasksInvoked = true
        }

        val shutdownTasks = mutableListOf<ContainerLifecycleListener>()

        val startupTasks = listeners.sortedBy { it.lifecycleListenerPriority }
        // TODO kilogolni startupTasks-ot

        startupTasks.forEach { listener ->
            try {
                listener.onContainerStartup()
                shutdownTasks.add(listener)
            } catch (e: Throwable) {
                e.nonFatalOrThrow().also {
                    // TODO log
                    runShutdownTasks() // FIXME ez így nem jó, mert ez a this.shutdownTasks-ot használja, ami itt még mindig üres
                    throw RuntimeException("Startup task failed: $listener", it)
                }
            }
        }

        this.shutdownTasks = shutdownTasks.toImmutableList()
    }

    suspend fun runShutdownTasks() {
        if (runShutdownTasksInvoked.compareAndSet(false, true)) {
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
            shutdownTasks = emptyImmutableList()
        }
    }

    override fun initiateShutdown() {
        TODO()
    }
}
