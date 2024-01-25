package xyz.kotlinw.module.pwa.server

import io.ktor.server.sessions.SessionStorage

fun interface SessionStorageBackendProvider {

    fun createSessionStorageBackend(): SessionStorage
}
