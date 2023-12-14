package xyz.kotlinw.module.webapp.core

import kotlinw.i18n.LocaleId
import kotlinx.serialization.Serializable
import xyz.kotlinw.module.auth.core.AuthenticationStatus

@Serializable
data class InitialWebAppClientEnvironmentData(
    val localeId: LocaleId,
    val authenticationStatus: AuthenticationStatus
)
