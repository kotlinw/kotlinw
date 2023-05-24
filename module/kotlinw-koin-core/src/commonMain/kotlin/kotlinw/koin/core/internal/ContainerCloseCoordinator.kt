package kotlinw.koin.core.internal

import kotlinw.collection.ArrayStack
import kotlinw.collection.MutableStack
import org.koin.core.scope.Scope

typealias OnCloseTask<T> = (T) -> Unit

@OptIn(ExperimentalStdlibApi::class)
interface ContainerCloseCoordinator : AutoCloseable {

    fun <T : Any> registerOnCloseTask(instance: T, onCloseTask: OnCloseTask<T>)
}

// TODO context(Scope)
fun <T : Any> T.registerOnCloseTask(scope: Scope, onCloseTask: OnCloseTask<T>): T {
    scope.get<ContainerCloseCoordinator>().registerOnCloseTask(this, onCloseTask)
    return this
}

internal class ContainerCloseCoordinatorImpl : ContainerCloseCoordinator {

    private data class RegisteredOnCloseTask<T : Any>(val instance: T, val onCloseTask: OnCloseTask<T>)

    private val registeredTasks: MutableStack<RegisteredOnCloseTask<Any>> = ArrayStack<RegisteredOnCloseTask<Any>>()

    override fun <T : Any> registerOnCloseTask(instance: T, onCloseTask: OnCloseTask<T>) {
        registeredTasks.push(RegisteredOnCloseTask(instance, onCloseTask) as RegisteredOnCloseTask<Any>)
    }

    override fun close() {
        while (registeredTasks.isNotEmpty()) {
            try {
                registeredTasks.popOrNull()?.also {
                    it.onCloseTask(it.instance)
                }
            } catch (e: Exception) {
                // TODO log
            }
        }
    }
}
