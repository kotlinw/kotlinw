package xyz.kotlinw.module.pwa.server

import io.ktor.server.auth.Principal

data class WebAppPrincipal(val userId: String, val userName: String) : Principal
