package xyz.kotlinw.module.auth.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.kotlinw.module.auth.core.AuthenticationStatus.Authenticated

@Serializable
sealed class AuthenticationStatus {

    abstract val permissions: Set<PermissionKey>

    @Serializable
    @SerialName("NotAuthenticated")
    data class NotAuthenticated(override val permissions: Set<PermissionKey>) : AuthenticationStatus()

    @Serializable
    @SerialName("Authenticated")
    data class Authenticated(
        val principal: String,
        val displayName: String?,
        override val permissions: Set<PermissionKey> // TODO nem biztos, hogy ezt így el kellene tárolni, sok helyet foglal
    ) :
        AuthenticationStatus()
}

inline val AuthenticationStatus.isAuthenticated: Boolean get() = this is Authenticated
