package xyz.kotlinw.module.webapp.client

import kotlinw.i18n.LocaleId
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.deserialize
import kotlinx.atomicfu.atomic
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.auth.core.AuthenticationStatus
import xyz.kotlinw.module.webapp.client.NetworkConnectionStatus.Connected
import xyz.kotlinw.module.webapp.client.NetworkConnectionStatus.NotConnected
import xyz.kotlinw.module.webapp.core.InitialWebAppClientEnvironmentData
import xyz.kotlinw.module.webapp.core.initialWebAppClientEnvironmentJsDataVariableName

@Component
class WebAppClientEnvironmentProviderImpl(
    serializerService: SerializerService
) : WebAppClientEnvironmentProvider {

    private val localeIdFlow: MutableStateFlow<LocaleId>

    private val _networkConnectionStateFlow = MutableStateFlow(NotConnected)

    private val _authenticationStateFlow: MutableStateFlow<AuthenticationStatus>

    init {
        serializerService.deserialize<InitialWebAppClientEnvironmentData>(
            window.asDynamic()[initialWebAppClientEnvironmentJsDataVariableName].unsafeCast<String>()
        )
            .also {
                localeIdFlow = MutableStateFlow(it.localeId)
                _authenticationStateFlow = MutableStateFlow(it.authenticationStatus)
            }
    }

    override val localeId = localeIdFlow.asStateFlow()

    override val authenticationState = _authenticationStateFlow.asStateFlow()

    override val networkConnectionState = _networkConnectionStateFlow.asStateFlow()

    override fun setNetworkConnectionStatus(status: NetworkConnectionStatus) {
        _networkConnectionStateFlow.value = status
    }
}
