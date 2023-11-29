package xyz.kotlinw.di.api

import kotlinw.util.stdlib.Priority

interface ContainerLifecycleListener {

    val lifecycleListenerPriority: Priority

    suspend fun onContainerStartup() {}

    suspend fun onContainerShutdown() {}
}
