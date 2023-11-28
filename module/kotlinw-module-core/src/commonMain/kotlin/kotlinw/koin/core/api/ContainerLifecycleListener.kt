package kotlinw.koin.core.api

import kotlinw.util.stdlib.Priority

interface ContainerLifecycleListener {

    val lifecycleListenerPriority: Priority

    suspend fun onContainerStartup()

    suspend fun onContainerShutdown()
}
