package xyz.kotlinw.module.webapp.core

import kotlinw.i18n.LocaleId

interface WebApplicationData {

    val localeId: LocaleId

    val authenticationData: AuthenticationData?

    interface AuthenticationData {

        val userName: String
    }
}
