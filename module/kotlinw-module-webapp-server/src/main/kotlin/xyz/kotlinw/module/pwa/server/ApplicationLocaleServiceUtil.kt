package xyz.kotlinw.module.pwa.server

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.acceptLanguageItems
import kotlinw.i18n.ApplicationLocaleService
import kotlinw.i18n.LocaleId
import kotlinw.i18n.LocaleIds
import kotlinw.i18n.findBestSupportedLocale

context(ApplicationLocaleService)
fun ApplicationCall.findBestSupportedLocale(): LocaleId =
    findBestSupportedLocale(
        request.acceptLanguageItems()
            .filter { it.value != "*" }
            .map { LocaleIds.of(it.value, fallbackLocaleId) }
    )
