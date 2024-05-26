package xyz.kotlinw.module.webapp.client

import kotlinw.i18n.LocaleId
import kotlinx.coroutines.flow.StateFlow
import xyz.kotlinw.module.auth.core.AuthenticationStatus

interface WebAppClientEnvironmentProvider {

    val localeId: StateFlow<LocaleId>

    val authenticationState: StateFlow<AuthenticationStatus>

    val networkConnectionState: StateFlow<NetworkConnectionStatus>

    fun setNetworkConnectionStatus(status: NetworkConnectionStatus)
}
