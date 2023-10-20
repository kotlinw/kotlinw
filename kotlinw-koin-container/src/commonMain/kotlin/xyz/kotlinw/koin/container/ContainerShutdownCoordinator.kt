package xyz.kotlinw.koin.container

import kotlinw.collection.ArrayStack
import kotlinw.collection.MutableStack

typealias OnShutdownTask<T> = (T) -> Unit

@OptIn(ExperimentalStdlibApi::class)
interface ContainerShutdownCoordinator : AutoCloseable {

    fun <T : Any> registerOnShutdownTask(instance: T, onShutdownTask: OnShutdownTask<T>)
}

class ContainerShutdownCoordinatorImpl : ContainerShutdownCoordinator {

    private data class RegisteredOnShutdownTask<T : Any>(val instance: T, val onShutdownTask: OnShutdownTask<T>)

    private val registeredTasks: MutableStack<RegisteredOnShutdownTask<Any>> = ArrayStack()

    override fun <T : Any> registerOnShutdownTask(instance: T, onShutdownTask: OnShutdownTask<T>) {
        registeredTasks.push(RegisteredOnShutdownTask(instance, onShutdownTask) as RegisteredOnShutdownTask<Any>)
    }

    override fun close() {
        while (registeredTasks.isNotEmpty()) {
            try {
                registeredTasks.popOrNull()?.also {
                    it.onShutdownTask(it.instance)
                }
            } catch (e: Exception) {
                // TODO log
            }
        }
    }
}
