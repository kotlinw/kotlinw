package kotlinw.i18n

interface ApplicationLocaleService {

    val fallbackLocaleId: LocaleId

    val supportedLocales: List<LocaleId>

    // TODO suspend fun <T> withLocaleContext(block: suspend LocaleContext.() -> T): T
}

fun ApplicationLocaleService.findBestSupportedLocale(localeIds: List<LocaleId>): LocaleId {
    return localeIds.firstOrNull { supportedLocales.contains(it) } ?: fallbackLocaleId
}
