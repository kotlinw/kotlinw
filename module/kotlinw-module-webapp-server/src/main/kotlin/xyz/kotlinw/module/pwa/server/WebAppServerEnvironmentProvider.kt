package xyz.kotlinw.module.pwa.server

import io.ktor.server.application.ApplicationCall
import kotlinw.i18n.LocaleId
import xyz.kotlinw.module.auth.core.AuthenticationStatus

interface WebAppServerEnvironmentProvider {

    context(ApplicationCall)
    val localeId: LocaleId

    context(ApplicationCall)
    val authenticationStatus: AuthenticationStatus
}
