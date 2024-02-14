package xyz.kotlinw.module.webapp.client

import xyz.kotlinw.module.auth.core.AuthenticationStatus

fun interface AuthenticationStatusProvider {

    suspend fun getAuthenticationStatus(lastAuthenticationStatus: AuthenticationStatus): AuthenticationStatus
}
