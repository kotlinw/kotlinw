package xyz.kotlinw.di.impl

import arrow.core.nonFatalOrThrow
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
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

    // TODO végignézni a hivatkozásokat, hogy jogosak-e, nem lehetne-e ContainerLifecycleListener-rel helyettesíteni őket
    fun <T : Any> registerListener( // TODO miért kell a :Any
        listenerId: String,
        onStartup: suspend () -> T,
        onShutdown: suspend (T) -> Unit,
        priority: Priority = Priority.Normal
    )

    fun initiateShutdown()
}

fun <T : Any> ContainerLifecycleCoordinator.registerListener(
    inlineComponentFactory: KFunction<*>, // TODO ehelyett a ComponentId-t kellene használni
    onStartup: suspend () -> T,
    onShutdown: suspend (T) -> Unit,
    priority: Priority = Priority.Normal
) =
    registerListener(
        inlineComponentFactory.name,
        onStartup,
        onShutdown,
        priority
    )

interface ContainerLifecycleCoordinatorInternal {

    suspend fun runStartupTasks(startupTaskFilter: (String) -> Boolean)
}

class ContainerLifecycleCoordinatorImpl : ContainerLifecycleCoordinator, ContainerLifecycleCoordinatorInternal {

    private val listeners = mutableListOf<ContainerLifecycleListener>()

    val lifecycleListeners: List<ContainerLifecycleListener> get() = listeners

    private val runStartupTasksInvoked = atomic(false)

    private var shutdownTasks: MutableQueue<ContainerLifecycleListener> = LinkedQueue()

    private val runShutdownTasksInvoked = atomic(false)

    private val listenersLock = reentrantLock()

    override fun <T : Any> registerListener(
        listenerId: String,
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

                    override fun getLifecycleListenerId(): String = listenerId

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

    override suspend fun runStartupTasks(startupTaskFilter: (String) -> Boolean) {
        listenersLock.withLock {
            if (!runStartupTasksInvoked.compareAndSet(false, true)) {
                throw IllegalStateException("runStartupTasks() has already been called.")
            }
        }

        try {
            val startupTasks = listeners.sortedBy { it.lifecycleListenerPriority }
            println("Startup tasks:") // TODO log
            startupTasks.forEach {
                val lifecycleListenerId = it.getLifecycleListenerId()
                val isEnabled = startupTaskFilter(lifecycleListenerId)
                println("  - ${if (isEnabled) "" else "IGNORED "}$lifecycleListenerId (${it.lifecycleListenerPriority.value})")
            }

            startupTasks.forEach { listener ->
                val lifecycleListenerId = listener.getLifecycleListenerId()
                val isEnabled = startupTaskFilter(lifecycleListenerId)
                if (isEnabled) {
                    try {
                        listener.onContainerStartup()// TODO log
                        shutdownTasks.add(listener)
                    } catch (e: Throwable) {
                        e.nonFatalOrThrow().also {
                            // TODO log
                            runShutdownTasks()
                            throw RuntimeException("Startup task failed: $listener", it)
                        }
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
                            it.onContainerShutdown()// TODO log
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
