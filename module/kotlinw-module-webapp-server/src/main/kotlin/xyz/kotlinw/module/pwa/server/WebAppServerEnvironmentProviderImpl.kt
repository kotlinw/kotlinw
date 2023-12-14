package xyz.kotlinw.module.pwa.server

import io.ktor.server.application.ApplicationCall
import kotlinw.i18n.ApplicationLocaleService
import kotlinw.i18n.LocaleId
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.auth.core.AuthenticationStatus

@Component
class WebAppServerEnvironmentProviderImpl(
    private val applicationLocaleService: ApplicationLocaleService,
    private val webSessionService: WebSessionService
) : WebAppServerEnvironmentProvider {

    context(ApplicationCall)
    override val localeId: LocaleId
        get() = webSessionService.sessionLocaleId ?: with(applicationLocaleService) { findBestSupportedLocale() }

    context(ApplicationCall)
    override val authenticationStatus: AuthenticationStatus
        get() = webSessionService.sessionAuthenticationStatus
}
