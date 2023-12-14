package xyz.kotlinw.module.webapp.client

import kotlinw.i18n.LocaleId
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.deserialize
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.auth.core.AuthenticationStatus
import xyz.kotlinw.module.webapp.core.InitialWebAppClientEnvironmentData
import xyz.kotlinw.module.webapp.core.initialWebAppClientEnvironmentJsDataVariableName

@Component
class WebAppClientEnvironmentProviderImpl(
    serializerService: SerializerService
) : WebAppClientEnvironmentProvider {

    private val localeIdFlow: MutableStateFlow<LocaleId>

    private val authenticationStatusFlow: MutableStateFlow<AuthenticationStatus>

    init {
        serializerService.deserialize<InitialWebAppClientEnvironmentData>(window.asDynamic()[initialWebAppClientEnvironmentJsDataVariableName].unsafeCast<String>())
            .also {
                localeIdFlow = MutableStateFlow(it.localeId)
                authenticationStatusFlow = MutableStateFlow(it.authenticationStatus)
            }
    }

    override val localeId: StateFlow<LocaleId> get() = localeIdFlow

    override val authenticationStatus: StateFlow<AuthenticationStatus> get() = authenticationStatusFlow
}
