package xyz.kotlinw.module.webapp.core

import kotlinw.i18n.LocaleId
import kotlinx.serialization.Serializable

@Serializable
data class DefaultWebApplicationData(
    override val localeId: LocaleId,
    override val authenticationData: DefaultAuthenticationData?
) : WebApplicationData {

    @Serializable
    data class DefaultAuthenticationData(
        override val userName: String
    ) : WebApplicationData.AuthenticationData
}
