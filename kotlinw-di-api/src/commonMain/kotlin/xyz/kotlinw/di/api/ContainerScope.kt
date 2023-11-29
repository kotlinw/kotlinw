package xyz.kotlinw.di.api

interface ContainerScope {

    suspend fun start()

    suspend fun close()
}

