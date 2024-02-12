package xyz.kotlinw.module.webapp.client

import arrow.core.nonFatalOrThrow
import kotlinw.i18n.LocaleId
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.deserialize
import kotlinx.atomicfu.atomic
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import web.navigator.Navigator
import web.navigator.navigator
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.auth.core.AuthenticationStatus
import xyz.kotlinw.module.auth.core.AuthenticationStatus.NotAuthenticated
import xyz.kotlinw.module.core.ApplicationCoroutineService
import xyz.kotlinw.module.webapp.client.NetworkConnectionStatus.Connected
import xyz.kotlinw.module.webapp.client.NetworkConnectionStatus.NotConnected
import xyz.kotlinw.module.webapp.client.NetworkConnectionStatus.UpdatingStatus
import xyz.kotlinw.module.webapp.core.InitialWebAppClientEnvironmentData
import xyz.kotlinw.module.webapp.core.initialWebAppClientEnvironmentJsDataVariableName

@Component
class WebAppClientEnvironmentProviderImpl(
    serializerService: SerializerService,
    private val authenticationStatusProvider: AuthenticationStatusProvider,
    private val applicationCoroutineService: ApplicationCoroutineService
) : WebAppClientEnvironmentProvider {

    private val localeIdFlow: MutableStateFlow<LocaleId>

    private val _networkConnectionStateFlow =
        MutableStateFlow(navigator.networkConnectionState)

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

    private var authenticationStatusRefresherJob by atomic<Job?>(null)

    override val networkConnectionState = _networkConnectionStateFlow.asStateFlow()

    init {
        window.addEventListener("online", { setNetworkConnectionState(Connected) })
        window.addEventListener("offline", { setNetworkConnectionState(NotConnected) })
    }

    private fun setNetworkConnectionState(status: NetworkConnectionStatus) {
        if (networkConnectionState.value != UpdatingStatus) {
            _networkConnectionStateFlow.value = status
        }
    }

    override suspend fun collectClientEnvironmentStatus() {
        if (authenticationStatusRefresherJob == null) {
            _networkConnectionStateFlow.value = UpdatingStatus
            authenticationStatusRefresherJob =
                applicationCoroutineService.applicationCoroutineScope.launch(start = UNDISPATCHED) {
                    _authenticationStateFlow.value =
                        try {
                            authenticationStatusProvider.getAuthenticationStatus()
                        } catch (e: Throwable) {
                            e.nonFatalOrThrow()
                            NotAuthenticated(emptySet()) // TODO jó ez? gyanús, hogy nem minden esetben, pl. ha nem fut a szerver, attól még authentikált marad az emberünk
                        } finally {
                            _networkConnectionStateFlow.value = navigator.networkConnectionState
                        }
                }
        }

        authenticationStatusRefresherJob!!.join()
    }
}

private val Navigator.networkConnectionState get() = if (onLine) Connected else NotConnected
