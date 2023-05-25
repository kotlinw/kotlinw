package kotlinw.koin.core.internal

import kotlinw.collection.ArrayQueue
import kotlinw.collection.ArrayStack
import kotlinw.collection.MutableQueue
import kotlinw.collection.MutableStack
import org.koin.core.scope.Scope

typealias OnStartupTask<T> = (T) -> Unit

interface ContainerStartupCoordinator {

    fun <T : Any> registerOnStartupTask(instance: T, onStartupTask: OnStartupTask<T>)

    fun runStartupTasks()
}

internal class ContainerStartupCoordinatorImpl : ContainerStartupCoordinator {

    private data class RegisteredOnStartupTask<T : Any>(val instance: T, val onStartupTask: OnStartupTask<T>)

    private val registeredTasks: MutableQueue<RegisteredOnStartupTask<Any>> = ArrayQueue()

    override fun <T : Any> registerOnStartupTask(instance: T, onStartupTask: OnStartupTask<T>) {
        registeredTasks.enqueue(RegisteredOnStartupTask(instance, onStartupTask) as RegisteredOnStartupTask<Any>)
    }

    override fun runStartupTasks() {
        while (registeredTasks.isNotEmpty()) {
            try {
                registeredTasks.dequeueOrNull()?.also {
                    it.onStartupTask(it.instance)
                }
            } catch (e: Exception) {
                // TODO log
            }
        }
    }
}
