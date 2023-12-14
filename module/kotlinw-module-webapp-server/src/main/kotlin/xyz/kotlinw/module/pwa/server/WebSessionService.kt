package xyz.kotlinw.module.pwa.server

import io.ktor.server.application.ApplicationCall
import kotlinw.i18n.LocaleId
import xyz.kotlinw.module.auth.core.AuthenticationStatus

interface WebSessionService {

    context(ApplicationCall)
    val sessionLocaleId: LocaleId?

    context(ApplicationCall)
    val sessionAuthenticationStatus: AuthenticationStatus
}
