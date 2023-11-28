package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.acceptLanguageItems
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinw.i18n.ApplicationLocaleService
import kotlinw.i18n.LocaleIds
import kotlinw.i18n.findBestSupportedLocale
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.pwa.core.WebManifestFactory

@Component
class WebManifestHttpController(
    private val webManifestFactory: WebManifestFactory,
    private val applicationLocaleService: ApplicationLocaleService
) : KtorServerApplicationConfigurer() {

    override fun Context.setup() {
        application.routing {
            get("/manifest.webmanifest") {
                val localeId =
                    applicationLocaleService.findBestSupportedLocale(
                        call.request.acceptLanguageItems()
                            .filter { it.value != "*" }
                            .map { LocaleIds.of(it.value, applicationLocaleService.fallbackLocaleId) }
                    )
                call.respondText(
                    webManifestFactory.createWebManifest(localeId).serializeToString(),
                    ContentType.parse("application/manifest+json")
                )
            }
        }
    }
}
