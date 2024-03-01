package xyz.kotlinw.module.pwa.server

import io.ktor.server.sessions.SessionStorage

fun interface SessionStorageBackendProvider {

    suspend fun createSessionStorageBackend(): SessionStorage
}
